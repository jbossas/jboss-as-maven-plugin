/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.plugin.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.as.plugin.common.DeploymentFailureException;

/**
 * Abstraction for GAV-based artifact deployments.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public abstract class AbstractArtifactDeployment extends AbstractDeployment {

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
     * The artifact to deploys groupId
     */
    @Parameter
    private String groupId;

    /**
     * The artifact to deploys artifactId
     */
    @Parameter
    private String artifactId;

    /**
     * The artifact to deploys version
     */
    @Parameter(required = false)
    private String version;

    /**
     * The artifact to deploys type
     */
    @Parameter(alias = "type", required = false)
    private String packagingType;

    /**
     * The artifact to deploys classifier
     */
    @Parameter(required = false)
    private String classifier;

    private Artifact artifact;

    /**
     * The resolved dependency file
     */
    @Parameter
    private File file;

    @Override
    public void validate() throws DeploymentFailureException {

        super.validate();

        if (file == null) {
            resolveArtifact();
        } else if (!file.exists()) {
            throw new DeploymentFailureException(String.format("File does not exist: %s", file.getName()));
        }
    }

    private void resolveArtifact() throws DeploymentFailureException {
        if (getArtifactId() == null) {
            throw new DeploymentFailureException("must specify the artifactId");
        }
        if (getGroupId() == null) {
            throw new DeploymentFailureException(" must specify the groupId");
        }

        @SuppressWarnings("unchecked")
        final Set<Artifact> dependencies = getProject().getArtifacts();
        artifact = null;
        for (final Artifact a : dependencies) {
            if (matches(a)) {
                artifact = a;
                break;
            }
        }

        // Resolve from Repository.
        if (artifact == null) {
            if (getVersion() == null) {
                throw new DeploymentFailureException("must specify the version");
            }
            VersionRange vr;
            try {
                vr = VersionRange.createFromVersionSpec(getVersion());
            } catch (InvalidVersionSpecificationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                vr = VersionRange.createFromVersion(getVersion());
            }

            if (StringUtils.isEmpty(getClassifier())) {
                artifact = factory.createDependencyArtifact(getGroupId(), getArtifactId(), vr, getPackagingType(), null,
                        Artifact.SCOPE_COMPILE);
            } else {
                artifact = factory.createDependencyArtifact(getGroupId(), getArtifactId(), vr, getPackagingType(),
                        getClassifier(), Artifact.SCOPE_COMPILE);
            }

        }

        if (artifact == null) {
            throw new DeploymentFailureException(String.format("Could not resolve artifact to deploy %s:%s:%s:%s"),
                    getGroupId(), getArtifactId(), getVersion(), getType());
        }

        if (artifact.getFile() == null) {
            List<ArtifactRepository> repoList = new ArrayList<ArtifactRepository>();

            if (pomRemoteRepositories != null) {
                repoList.addAll(pomRemoteRepositories);
            }

            try {
                artifactResolver.resolve(artifact, repoList, localRepository);
            } catch (ArtifactResolutionException e) {
                throw new DeploymentFailureException(e.getMessage(), e);
            } catch (ArtifactNotFoundException e) {
                throw new DeploymentFailureException(e.getMessage(), e);
            }
        }
        file = artifact.getFile();
    }

    private boolean matches(Artifact a) {

        if (a.getArtifactId().equals(getArtifactId()) && a.getGroupId().equals(getVersion())) {

            boolean matchClassifier = false;
            boolean matchType = false;

            boolean matchVersion = matchesVersion(a);

            if (matchClassifier && matchType && matchVersion) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesVersion(Artifact a) {

        boolean matchVersion = false;
        if (getVersion() != null) {
            if (getVersion().equals(a.getVersion())) {
                matchVersion = true;
            }
        } else {
            matchVersion = true;
        }
        return matchVersion;
    }

    private boolean matchesClassifier(Artifact a) {

        boolean matchClassifier = false;
        if (getClassifier() != null) {
            if (getClassifier().equals(a.getClassifier())) {
                matchClassifier = true;
            }
        } else {
            matchClassifier = true;
        }
        return matchClassifier;
    }

    private boolean matchesType(Artifact a) {

        boolean matchType = false;
        if (getPackagingType() != null) {
            if (getPackagingType().equals(a.getType())) {
                matchType = true;
            }
        } else {
            matchType = true;
        }
        return matchType;
    }

    public String getGroupId() {

        return groupId;
    }

    public void setGroupId(String groupId) {

        this.groupId = groupId;
    }

    public String getArtifactId() {

        return artifactId;
    }

    public void setArtifactId(String artifactId) {

        this.artifactId = artifactId;
    }

    public File getFile() {

        return file;
    }

    public void setFile(File file) {

        this.file = file;
    }

    public MavenProject getProject() {

        return project;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public String getPackagingType() {

        return packagingType;
    }

    public void setPackagingType(String packagingType) {

        this.packagingType = packagingType;
    }

    public String getClassifier() {

        return classifier;
    }

    public void setClassifier(String classifier) {

        this.classifier = classifier;
    }

    @Override
    protected File file() {

        return file;
    }
}
