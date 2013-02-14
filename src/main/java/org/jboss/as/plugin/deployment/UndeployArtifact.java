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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.as.plugin.deployment.Deployment.Type;

/**
 * Undeploys (removes) an arbitrary artifact to the JBoss application server
 * 
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "undeploy-artifact", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public final class UndeployArtifact extends AbstractArtifactDeployment {

    /**
     * Indicates whether undeploy should ignore the undeploy operation if the deployment does not exist.
     */
    @Parameter(defaultValue = "true", property = "undeploy.ignoreMissingDeployment")
    private boolean ignoreMissingDeployment;

    @Override
    public String goal() {

        return "undeploy-artifact";
    }

    @Override
    public Type getType() {

        return (ignoreMissingDeployment ? Type.UNDEPLOY_IGNORE_MISSING : Type.UNDEPLOY);
    }
}
