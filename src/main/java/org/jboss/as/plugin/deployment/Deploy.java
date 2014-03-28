/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jboss.as.plugin.common.PropertyNames;
import org.jboss.as.plugin.deployment.Deployment.Type;

/**
 * Deploys the application to the JBoss Application Server.
 * <p/>
 * If {@code force} is set to {@code true}, the server is queried to see if the application already exists. If the
 * application already exists, the application is redeployed instead of deployed. If the application does not exist the
 * application is deployed as normal.
 * <p/>
 * If {@code force} is set to {@code false} and the application has already been deployed to the server, an error
 * will occur and the deployment will fail.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class Deploy extends AbstractAppDeployment {

    /**
     * Specifies whether force mode should be used or not.
     * </p>
     * If force mode is disabled, the deploy goal will cause a build failure if the application being deployed already
     * exists.
     */
    @Parameter(defaultValue = "true", property = PropertyNames.DEPLOY_FORCE)
    private boolean force;

    /**
     * Specifies whether the deployed application should be automatically enabled or not.
     * <p>
     * If enabled, the deploy goal will automatically run the application in the target container. If disabled, the
     * content will be uploaded but not deployed.
     * <p>
     * Note that if an application of the same name is already running and the <tt>force</tt> parameter is true, the
     * application will be enabled automatically, even if this parameter is false (disabled). That is, an enabled
     * application will not be disabled by re-deployment. The converse is true, i.e. a disabled application may be
     * enabled by a forced deployment of the same content where this parameter is true.
     */
    @Parameter(defaultValue = "true", property = PropertyNames.DEPLOY_ENABLED)
    private boolean deployEnabled = true;

    @Override
    public String goal() {
        return "deploy";
    }

    @Override
    public Type getType() {
        if (deployEnabled) {
            return (force ? Type.FORCE_DEPLOY : Type.DEPLOY);
        } else {
            return (force ? Type.FORCE_ADD : Type.ADD);
        }
    }

}