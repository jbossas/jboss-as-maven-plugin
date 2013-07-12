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

package org.jboss.as.plugin.common;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Utilty class to manage server distribution download and extraction
 * @authorAkram Ben Aissi
 */

public class Distribution {
    public static final String JBOSS_DIR = "jboss-as-run";

    public static File extractIfRequired(final File buildDir, String jbossHome, String jbossAsArtifact, String version,
            List<RemoteRepository> remoteRepos, RepositorySystemSession repoSession, RepositorySystem repoSystem)
            throws MojoFailureException, MojoExecutionException {
        if (jbossHome != null) {
            // we do not need to download JBoss
            return new File(jbossHome);
        }
        final ArtifactRequest request = new ArtifactRequest();
        DefaultArtifact defaultArtifact = new DefaultArtifact(jbossAsArtifact);
        // JBASMP-49 - Give the ability to specify the jboss-as dist artifact
        // For compatiblity reasons, if version is also passed as paremeter, it
        // overrides the version passed in defaultArtifact.
        if (version != null) {
            defaultArtifact.setVersion(version);
        }
        request.setArtifact(defaultArtifact);
        request.setRepositories(remoteRepos);
        // getLog().info(String.format("Resolving artifact %s from %s",
        // jbossAsArtifact, remoteRepos));
        final ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        final File target = new File(buildDir, JBOSS_DIR);
        Streams.unzip(result.getArtifact().getFile(), target);

        return new File(target.getAbsoluteFile(), String.format("jboss-as-%s", result.getArtifact().getVersion()));
    }

}
