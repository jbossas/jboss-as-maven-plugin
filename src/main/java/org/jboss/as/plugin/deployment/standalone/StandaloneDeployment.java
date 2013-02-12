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

package org.jboss.as.plugin.deployment.standalone;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult;
import org.jboss.as.plugin.common.DeploymentExecutionException;
import org.jboss.as.plugin.common.DeploymentFailureException;
import org.jboss.as.plugin.common.Operations;
import org.jboss.as.plugin.deployment.Deployment;
import org.jboss.dmr.ModelNode;

/**
 * A deployment for standalone servers.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StandaloneDeployment implements Deployment {

    private final File content;
    private final ModelControllerClient client;
    private final String name;
    private final Type type;
    private final String replacementPattern;

    /**
     * Creates a new deployment.
     *
     * @param client the client that is connected.
     * @param content the content for the deployment.
     * @param name the name of the deployment, if {@code null} the name of the content file is used.
     * @param type the deployment type.
     * @param replacementPattern the replacement pattern (old artifact name)
     */
    public StandaloneDeployment(final ModelControllerClient client, final File content, final String name,
                                final String replacementPattern, final Type type) {
        this.content = content;
        this.client = client;
        this.name = (name == null ? content.getName() : name);
        this.type = type;
        this.replacementPattern = (replacementPattern == null ? name : replacementPattern);
    }

    /**
     * Creates a new deployment.
     *
     * @param client the client that is connected.
     * @param content the content for the deployment.
     * @param name the name of the deployment, if {@code null} the name of the content file is used.
     * @param type the deployment type.
     * @param replacementPattern the replacement pattern (old artifact name)
     * @return the new deployment
     */
    public static StandaloneDeployment create(final ModelControllerClient client, final File content, final String name,
                                              final String replacementPattern, final Type type) {
        return new StandaloneDeployment(client, content, name, replacementPattern, type);
    }

    private DeploymentPlan createPlan(final DeploymentPlanBuilder builder) throws IOException {
        DeploymentPlanBuilder planBuilder = builder;
        switch (type) {
            case DEPLOY: {
                planBuilder = builder.add(name, content).andDeploy();
                break;
            }
            case REDEPLOY: {
                planBuilder = builder.replace(getDeploymentName(replacementPattern), content).redeploy(name);
                break;
            }
            case UNDEPLOY: {
                planBuilder = builder.undeploy(getDeploymentName(replacementPattern)).remove(getDeploymentName(replacementPattern));
                break;
            }
            case FORCE_DEPLOY: {
                if (exists()) {
                    planBuilder = builder.replace(getDeploymentName(replacementPattern), content).redeploy(name);
                } else {
                    planBuilder = builder.add(name, content).andDeploy();
                }
                break;
            }
            case UNDEPLOY_IGNORE_MISSING: {
                if (exists()) {
                    planBuilder = builder.undeploy(getDeploymentName(replacementPattern)).remove(getDeploymentName(replacementPattern));
                } else {
                    return null;
                }
                break;
            }
        }
        return planBuilder.build();
    }


    @Override
    public Status execute() throws DeploymentExecutionException, DeploymentFailureException {
        Status resultStatus = Status.SUCCESS;
        try {
            final ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(client);
            final DeploymentPlanBuilder builder = manager.newDeploymentPlan();
            final DeploymentPlan plan = createPlan(builder);
            if (plan != null) {
                if (plan.getDeploymentActions().size() > 0) {
                    final ServerDeploymentPlanResult planResult = manager.execute(plan).get();
                    // Check the results

                    boolean foundException = false;
                    for (DeploymentAction action : plan.getDeploymentActions()) {
                        final ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(action.getId());
                        if (actionResult.getDeploymentException() != null) {
                            resultStatus = getStatus(resultStatus, actionResult);
                        }
                    }

                    for (DeploymentAction action : plan.getDeploymentActions()) {
                        final ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(action.getId());
                        final ServerUpdateActionResult.Result result = actionResult.getResult();
                        resultStatus = getStatus(resultStatus, actionResult);
                    }

                }
            }
        } catch (DeploymentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentExecutionException(e, "Error executing %s", type);
        }
        return resultStatus;
    }

    private Status getStatus(Status resultStatus, ServerDeploymentActionResult actionResult)
            throws DeploymentExecutionException {
        final ServerUpdateActionResult.Result result = actionResult.getResult();
        switch (result) {
            case FAILED:
                throw new DeploymentExecutionException("Deployment failed.", actionResult.getDeploymentException());
            case NOT_EXECUTED:
                throw new DeploymentExecutionException("Deployment not executed.", actionResult.getDeploymentException());
            case ROLLED_BACK:
                throw new DeploymentExecutionException("Deployment failed and was rolled back.", actionResult.getDeploymentException());
            case CONFIGURATION_MODIFIED_REQUIRES_RESTART:
                resultStatus = Status.REQUIRES_RESTART;
                break;
        }
        return resultStatus;
    }

    @Override
    public Type getType() {
        return type;
    }

    private boolean exists() {
        String deploymentName = getDeploymentName(replacementPattern);
        if (deploymentName != null) {
            return true;
        }

        return false;
    }


    private String getDeploymentName(String deploymentNamePattern) {
        // CLI :read-children-names(child-type=deployment)
        final ModelNode op = Operations.createListDeploymentsOperation();
        final ModelNode result;
        try {
            result = client.execute(op);
            final String deploymentName = name;
            // Check to make sure there is an outcome
            if (Operations.successful(result)) {
                final List<ModelNode> deployments = (result.hasDefined(Operations.RESULT) ? result.get(Operations.RESULT).asList() : Collections.<ModelNode>emptyList());
                for (ModelNode n : deployments) {
                    if (n.asString().matches(deploymentNamePattern)) {
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
}
