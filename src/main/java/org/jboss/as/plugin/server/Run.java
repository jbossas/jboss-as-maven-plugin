/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.plugin.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.plugin.common.DeploymentFailureException;
import org.jboss.as.plugin.common.Operations;
import org.jboss.as.plugin.common.Streams;
import org.jboss.as.plugin.deployment.Deploy;
import org.jboss.as.plugin.deployment.Deployment;
import org.jboss.as.plugin.deployment.standalone.StandaloneDeployment;
import org.jboss.jdf.stacks.client.StacksClient;
import org.jboss.jdf.stacks.model.Runtime;

/**
 * Starts a standalone instance of JBoss Application Server 7 and deploys the application to the server.
 * 
 * @author Stuart Douglas
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class Run extends Deploy {

    public static final String JBOSS_DIR = "jboss-as-run";

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory factory;

    @Component
    private ArtifactResolver artifactResolver;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> pomRemoteRepositories;

    /**
     * The JBoss Application Server's home directory. If not used, JBoss Application Server will be downloaded.
     */
    @Parameter(alias = "jboss-home", property = "jboss-as.home")
    private String jbossHome;

    /**
     * The version of the JBoss Application Server to run.
     */
    @Parameter(alias = "jboss-as-version", defaultValue = "7.1.1.Final", property = "jboss-as.version")
    private String version;

    /**
     * JBoss Runtime Download-Url (optional, in case JBoss Runtime cannot be validated against JDF Stacks)
     */
    @Parameter(alias = "dowload-url", property = "jboss-as.download.url")
    private String downloadUrl;

    /**
     * The modules path to use.
     */
    @Parameter(alias = "modules-path", property = "jboss-as.modulesPath")
    private String modulesPath;

    /**
     * The bundles path to use.
     */
    @Parameter(alias = "bundles-path", property = "jboss-as.bundlesPath")
    private String bundlesPath;

    /**
     * A space delimited list of JVM arguments.
     */
    @Parameter(alias = "jvm-args", property = "jboss-as.jvmArgs", defaultValue = "-Xms64m -Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000")
    private String jvmArgs;

    /**
     * The {@code JAVA_HOME} to use for launching the server.
     */
    @Parameter(alias = "java-home", property = "java.home")
    private String javaHome;

    /**
     * The path to the server configuration to use.
     */
    @Parameter(alias = "server-config", property = "jboss-as.serverConfig")
    private String serverConfig;

    /**
     * The timeout value to use when starting the server.
     */
    @Parameter(alias = "startup-timeout", defaultValue = "60", property = "jboss-as.startupTimeout")
    private long startupTimeout;

    @Override
    protected void doExecute() throws MojoExecutionException {
        final Log log = getLog();
        final File deploymentFile = file();
        final String deploymentName = deploymentFile.getName();
        final File targetDir = deploymentFile.getParentFile();
        // The deployment must exist before we do anything
        if (!deploymentFile.exists()) {
            throw new MojoExecutionException(String.format("The deployment '%s' could not be found.",
                    deploymentFile.getAbsolutePath()));
        }
        // Validate the environment
        final File jbossHome = extractIfRequired(targetDir);
        if (!jbossHome.isDirectory()) {
            throw new MojoExecutionException(String.format("JBOSS_HOME '%s' is not a valid directory.", jbossHome));
        }
        // JVM arguments should be space delimited
        final String[] jvmArgs = (this.jvmArgs == null ? null : this.jvmArgs.split("\\s+"));
        final String javaHome;
        if (this.javaHome == null) {
            javaHome = SecurityActions.getEnvironmentVariable("JAVA_HOME");
        } else {
            javaHome = this.javaHome;
        }
        final ServerInfo serverInfo = ServerInfo.of(this, javaHome, jbossHome, modulesPath, bundlesPath, jvmArgs, serverConfig,
                startupTimeout);
        if (!serverInfo.getModulesDir().isDirectory()) {
            throw new MojoExecutionException(String.format("Modules path '%s' is not a valid directory.", modulesPath));
        }
        if (!serverInfo.getBundlesDir().isDirectory()) {
            throw new MojoExecutionException(String.format("Bundles path '%s' is not a valid directory.", bundlesPath));
        }
        // Print some server information
        log.info(String.format("JAVA_HOME=%s", javaHome));
        log.info(String.format("JBOSS_HOME=%s%n", jbossHome));
        try {
            // Create the server
            final Server server = new StandaloneServer(serverInfo);
            final Thread shutdownThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    server.stop();
                    // Bad hack to get maven to complete it's message output
                    try {
                        TimeUnit.MILLISECONDS.sleep(500L);
                    } catch (InterruptedException ignore) {
                        // no-op
                    }
                }
            });
            // Add the shutdown hook
            SecurityActions.addShutdownHook(shutdownThread);
            // Start the server
            log.info("Server is starting up. Press CTRL + C to stop the server.");
            server.start();
            // Deploy the application
            server.checkServerState();
            if (server.isRunning()) {
                log.info(String.format("Deploying application '%s'%n", deploymentFile.getName()));
                final ModelControllerClient client = server.getClient();
                final Deployment deployment = StandaloneDeployment.create(client, deploymentFile, deploymentName, null,
                        getType());
                switch (executeDeployment(client, deployment)) {
                    case REQUIRES_RESTART: {
                        client.execute(Operations.createOperation(Operations.RELOAD));
                        break;
                    }
                    case SUCCESS:
                        break;
                }
            } else {
                throw new DeploymentFailureException("Cannot deploy to a server that is not running.");
            }
            while (server.isRunning()) {
                TimeUnit.SECONDS.sleep(1L);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("The server failed to start", e);
        }

    }

    private File extractIfRequired(final File buildDir) throws MojoExecutionException {
        if (jbossHome != null) {
            // we do not need to download JBoss
            return new File(jbossHome);
        }

        File compressedRuntime = null;
        Runtime runtime = getRuntime();
        if (runtime != null) {
            compressedRuntime = resolveFromMavenRepository(runtime);
        } else if (downloadUrl != null) {
            try {
                URL url = new URL(downloadUrl);
                compressedRuntime = new File(buildDir, url.getFile());
                if (!compressedRuntime.exists()) {
                    getLog().info(String.format("Downloading JBoss AS runtime from '%s'. This may take a while", downloadUrl));
                    FileUtils.copyURLToFile(url, compressedRuntime);
                }
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Cannot download JBoss AS runtime from URL '%s'", downloadUrl),
                        e);
            }
        } else {
            throw new MojoExecutionException("Cannot download JBoss AS runtime. Please specify either "
                    + "a valid version or downloadUrl parameter");
        }

        final File target = new File(buildDir, JBOSS_DIR);
        if (target.exists()) {
            target.delete();
        }
        decompressZipFile(compressedRuntime, target);

        return new File(target.getAbsoluteFile(), String.format("jboss-as-%s", version));
    }

    private File resolveFromMavenRepository(Runtime runtime) throws MojoExecutionException {
        File compressedRuntime;

        VersionRange vr;
        try {
            vr = VersionRange.createFromVersionSpec(runtime.getVersion());
        } catch (InvalidVersionSpecificationException e1) {
            getLog().debug(e1.getMessage(), e1);
            vr = VersionRange.createFromVersion(runtime.getVersion());
        }

        Artifact artifact = factory.createDependencyArtifact(runtime.getGroupId(), runtime.getArtifactId(), vr, "zip", null,
                org.apache.maven.artifact.Artifact.SCOPE_COMPILE);

        if (artifact.getFile() == null) {
            List<ArtifactRepository> repoList = new ArrayList<ArtifactRepository>();

            if (pomRemoteRepositories != null) {
                repoList.addAll(pomRemoteRepositories);
            }
            getLog().info(String.format("Resolving artifact %s from %s", artifact.toString(), pomRemoteRepositories));

            try {
                artifactResolver.resolve(artifact, repoList, localRepository);
            } catch (ArtifactResolutionException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } catch (ArtifactNotFoundException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

        }
        compressedRuntime = artifact.getFile();
        return compressedRuntime;
    }

    private void decompressZipFile(File input, File target) {
        final byte buff[] = new byte[1024];
        ZipFile file = null;
        try {
            file = new ZipFile(input);
            final Enumeration<? extends ZipEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final File extractTarget = new File(target.getAbsolutePath(), entry.getName());
                if (entry.isDirectory()) {
                    extractTarget.mkdirs();
                } else {
                    final File parent = new File(extractTarget.getParent());
                    parent.mkdirs();
                    final BufferedInputStream in = new BufferedInputStream(file.getInputStream(entry));
                    try {
                        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(extractTarget));
                        try {
                            int read = 0;
                            while ((read = in.read(buff)) != -1) {
                                out.write(buff, 0, read);
                            }
                        } finally {
                            Streams.safeClose(out);
                        }
                    } finally {
                        Streams.safeClose(in);

                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Error extracting '%s'",
                    (file == null ? "null file" : file.getName())), e);
        } finally {
            Streams.safeClose(file);
        }
    }

    @Override
    public String goal() {
        return "run";
    }

    public Runtime getRuntime() {

        StacksClient client = new StacksClient();
        Set<String> versions = new TreeSet<String>();

        if (validateVersion(client, version)) {

            List<Runtime> runtimes = client.getStacks().getAvailableRuntimes();
            for (Runtime runtime : runtimes) {
                if (runtime.getVersion().equals(version)) {
                    return runtime;
                }
                versions.add(runtime.getVersion());
            }
        }

        return null;
    }

    public boolean validateVersion(StacksClient client, String version) {

        List<Runtime> runtimes = client.getStacks().getAvailableRuntimes();
        Set<String> versions = new TreeSet<String>();

        for (Runtime runtime : runtimes) {
            if (runtime.getVersion().equals(version)) {

                if (runtime.getArtifactId() == null || runtime.getGroupId() == null) {
                    if (downloadUrl == null) {
                        getLog().info(
                                "Specified version %s is known, but has no Maven artifact coordinates for "
                                        + "downloading. Please specify downloadUrl");
                    }

                    return false;
                }
                return true;
            }
            versions.add(runtime.getVersion());
        }

        getLog().warn(String.format("Specified version %s is unknown. Known versions are: %s", version, versions));

        return false;
    }

}
