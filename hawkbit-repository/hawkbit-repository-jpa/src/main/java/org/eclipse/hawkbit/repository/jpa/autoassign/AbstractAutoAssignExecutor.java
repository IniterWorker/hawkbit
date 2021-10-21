/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;

/**
 * Abstract implementation of an AutoAssignExecutor
 */
public abstract class AbstractAutoAssignExecutor implements AutoAssignExecutor {

    /**
     * The message which is added to the action status when a distribution set
     * is assigned to an target. First %s is the name of the target filter.
     */
    private static final String ACTION_MESSAGE = "Auto assignment by target filter: %s";

    private final DeploymentManagement deploymentManagement;

    private final PlatformTransactionManager transactionManager;

    /**
     * Constructor
     * 
     * @param deploymentManagement
     *            to assign distribution sets to targets
     * @param transactionManager
     *            to run transactions
     */
    protected AbstractAutoAssignExecutor(final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager) {
        this.deploymentManagement = deploymentManagement;
        this.transactionManager = transactionManager;
    }

    protected DeploymentManagement getDeploymentManagement() {
        return deploymentManagement;
    }

    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Runs target assignments within a dedicated transaction for a given list
     * of controllerIDs
     * 
     * @param targetFilterQuery
     *            the target filter query
     * @param controllerIds
     *            the controllerIDs
     * @return count of targets
     */
    protected int runTransactionalAssignment(final TargetFilterQuery targetFilterQuery,
            final List<String> controllerIds) {
        final String actionMessage = String.format(ACTION_MESSAGE, targetFilterQuery.getName());

        return DeploymentHelper.runInNewTransaction(getTransactionManager(), "autoAssignDSToTargets",
                Isolation.READ_COMMITTED.value(), status -> {

                    final List<DeploymentRequest> deploymentRequests = mapToDeploymentRequests(controllerIds,
                            targetFilterQuery);

                    final int count = deploymentRequests.size();
                    if (count > 0) {
                        getDeploymentManagement().assignDistributionSets(
                                getAutoAssignmentInitiatedBy(targetFilterQuery), deploymentRequests, actionMessage);
                    }
                    return count;
                });
    }

    /**
     * Creates a list of {@link DeploymentRequest} for given list of
     * controllerIds and {@link TargetFilterQuery}
     *
     * @param controllerIds
     *            list of controllerIds
     * @param filterQuery
     *            the query the targets have to match
     * @return list of deployment request
     */
    protected List<DeploymentRequest> mapToDeploymentRequests(final List<String> controllerIds,
            final TargetFilterQuery filterQuery) {
        // the action type is set to FORCED per default (when not explicitly
        // specified)
        final Action.ActionType autoAssignActionType = filterQuery.getAutoAssignActionType() == null
                ? Action.ActionType.FORCED
                : filterQuery.getAutoAssignActionType();

        return controllerIds.stream()
                .map(controllerId -> DeploymentManagement
                        .deploymentRequest(controllerId, filterQuery.getAutoAssignDistributionSet().getId())
                        .setActionType(autoAssignActionType).setWeight(filterQuery.getAutoAssignWeight().orElse(null))
                        .build())
                .collect(Collectors.toList());
    }

    protected static String getAutoAssignmentInitiatedBy(final TargetFilterQuery targetFilterQuery) {
        return StringUtils.isEmpty(targetFilterQuery.getAutoAssignInitiatedBy()) ? targetFilterQuery.getCreatedBy()
                : targetFilterQuery.getAutoAssignInitiatedBy();
    }
}
