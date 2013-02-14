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

package org.jboss.as.plugin.common;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Utility to lookup up Deployments.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DeploymentInspector {

    /**
     * Utility Constructor.
     */
    private DeploymentInspector() {

    }

    /**
     * Lookup an existing Deployment using a static name or a pattern. At least deploymentName or
     * deploymentNamePattern must be set.
     * @param client
     * @param deploymentName Name for exact matching.
     * @param deploymentNamePattern Regex-Pattern for deployment matching.
     * @return the name of the deployment or null.
     */
    public static String getDeploymentName(ModelControllerClient client, String deploymentName, String deploymentNamePattern) {

        if (deploymentName == null && deploymentNamePattern == null) {
            throw new IllegalArgumentException("deploymentName and deploymentNamePattern are null. One of them must "
                    + "be set in order to find an existing deployment.");
        }

        // CLI :read-children-names(child-type=deployment)
        final ModelNode op = Operations.createListDeploymentsOperation();
        final ModelNode result;
        try {
            result = client.execute(op);
            // Check to make sure there is an outcome
            if (Operations.successful(result)) {
                final List<ModelNode> deployments = (result.hasDefined(Operations.RESULT) ? result.get(Operations.RESULT)
                        .asList() : Collections.<ModelNode> emptyList());
                for (ModelNode n : deployments) {
                    if (matches(n.asString(), deploymentName, deploymentNamePattern)) {
                        return n.asString();
                    }
                }
            } else {
                throw new IllegalStateException(Operations.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Could not execute operation '%s'", op), e);
        }
        return null;

    }

    private static boolean matches(String deploymentName, String targetDeploymentName, String deploymentNamePattern) {

        if (deploymentNamePattern != null) {
            return deploymentName.matches(deploymentNamePattern);
        }

        return targetDeploymentName.equals(deploymentName);
    }
}
