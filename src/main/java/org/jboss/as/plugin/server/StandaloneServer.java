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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.plugin.common.Files;
import org.jboss.as.plugin.common.IoUtils;
import org.jboss.as.plugin.common.ServerOperations;
import org.jboss.dmr.ModelNode;

/**
 * A standalone server.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class StandaloneServer extends Server {

    private static final String STARTING = "starting";
    private static final String STOPPING = "stopping";

    private final ServerConfig serverConfig;
    private boolean isRunning;
    private ModelControllerClient client;

    /**
     * Creates a new standalone server.
     *
     * @param serverConfig the configuration information for the server
     */
    public StandaloneServer(final ServerConfig serverConfig) {
        super(serverConfig, "JBAS015950");
        this.serverConfig = serverConfig;
        isRunning = false;
    }

    @Override
    protected void init() throws IOException {
        client = ModelControllerClient.Factory.create(serverConfig.getConnectionInfo().getHostAddress(), serverConfig.getConnectionInfo().getPort(), serverConfig.getConnectionInfo().getCallbackHandler());
    }

    @Override
    protected void stopServer() {
        try {
            if (client != null) {
                try {
                    client.execute(ServerOperations.createOperation(ServerOperations.SHUTDOWN));
                } catch (IOException e) {
                    // no-op
                } finally {
                    IoUtils.safeClose(client);
                    client = null;
                }
                try {
                    getConsole().awaitShutdown(5L);
                } catch (InterruptedException ignore) {
                    // no-op
                }
            }
        } finally {
            isRunning = false;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        if (isRunning) {
            return true;
        }
        checkServerState();
        return isRunning;
    }

    @Override
    public synchronized ModelControllerClient getClient() {
        return client;
    }

    @Override
    protected List<String> createLaunchCommand() {
        final File jbossHome = serverConfig.getJbossHome();
        final String javaHome = serverConfig.getJavaHome();
        final File modulesJar = new File(Files.createPath(jbossHome.getAbsolutePath(), "jboss-modules.jar"));
        if (!modulesJar.exists())
            throw new IllegalStateException("Cannot find: " + modulesJar);
        String javaExec = Files.createPath(javaHome, "bin", "java");
        if (javaHome.contains(" ")) {
            javaExec = "\"" + javaExec + "\"";
        }

        // Create the commands
        final List<String> cmd = new ArrayList<String>();
        cmd.add(javaExec);

        // Add the JVM args
        cmd.addAll(serverConfig.getJvmArgs());

        cmd.add("-Djboss.home.dir=" + jbossHome);
        cmd.add("-Dorg.jboss.boot.log.file=" + Files.createPath(serverConfig.getLogDir(), "boot.log"));
        cmd.add("-Dlogging.configuration=file:" + Files.createPath(serverConfig.getConfigDir(), "logging.properties"));
        //        cmd.add("-Djboss.modules.dir=" + serverInfo.getModulesDir());
        cmd.add("-Djboss.bundles.dir=" + serverConfig.getBundlesDir());
        cmd.add("-jar");
        cmd.add(modulesJar.getAbsolutePath());
        cmd.add("-mp");
        cmd.add(serverConfig.getModulesDir());
        cmd.add("-jaxpmodule");
        cmd.add("javax.xml.jaxp-provider");
        cmd.add("org.jboss.as.standalone");
        if (serverConfig.getServerConfig() != null) {
            cmd.add("-server-config");
            cmd.add(serverConfig.getServerConfig());
        }
        if (serverConfig.getPropertiesFile() != null) {
            cmd.add("-P");
            cmd.add(serverConfig.getPropertiesFile());
        }
        // Add all the server arguments
        cmd.addAll(serverConfig.getServerArgs());
        return cmd;
    }

    @Override
    protected void checkServerState() {
        if (client == null) {
            isRunning = false;
        } else {
            try {
                final ModelNode result = client.execute(ServerOperations.createReadAttributeOperation(ServerOperations.SERVER_STATE));
                System.out.println(result.asString());
                isRunning = ServerOperations.isSuccessfulOutcome(result) && !STARTING.equalsIgnoreCase(ServerOperations.readResultAsString(result)) &&
                        !STOPPING.equalsIgnoreCase(ServerOperations.readResultAsString(result));
            } catch (Throwable ignore) {
                isRunning = false;
            }
        }
    }

}
