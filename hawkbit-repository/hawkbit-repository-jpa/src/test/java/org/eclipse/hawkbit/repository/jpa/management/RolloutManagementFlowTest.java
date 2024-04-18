/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Junit tests for RolloutManagement.
 */
@Feature("Component Tests - Repository")
@Story("Rollout Management (Flow)")
class RolloutManagementFlowTest extends AbstractJpaIntegrationTest {

    @BeforeEach
    void reset() {
        this.approvalStrategy.setApprovalNeeded(false);
    }

    @Test
    @Description("Verifies a simple rollout flow")
    void rolloutFlow() {
        final String rolloutName = "rollout-std";
        final int amountGroups = 5; // static only
        final String targetPrefix = "controller-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30", false, false);
        final List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();

        // add 2 targets not to be included
        testdataFactory.createTargets(targetPrefix, amountGroups * 3, 2);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, i == 0 ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
        }

        executeStaticWithoutOneTargetFromTheLastGroup(groups, rollout, amountGroups);

        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.PAUSED, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, false, RolloutStatus.RUNNING, amountGroups, amountGroups * 3);
        assertAndGetRunning(rollout, 1); // keep running
    }

    @Test
    @Description("Verifies a simple dynamic rollout flow")
    void dynamicRolloutFlow() {
        final String rolloutName = "dynamic-rollout-std";
        final int amountGroups = 5; // static only
        final String targetPrefix = "controller-dynamic-rollout-std-";
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);

        testdataFactory.createTargets(targetPrefix, 0, amountGroups * 3);
        final Rollout rollout = testdataFactory.createRolloutByVariables(rolloutName, rolloutName, amountGroups,
                "controllerid==" + targetPrefix + "*", distributionSet, "60", "30", false, true);

        // rollout is READY
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3);
        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic1 = groups.get(amountGroups);
        assertRollout(rollout, true, RolloutStatus.READY, amountGroups + 1, amountGroups * 3); // + dynamic
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, RolloutGroupStatus.READY, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.READY, 0);

        // add 2 targets for the first dynamic group
        testdataFactory.createTargets(targetPrefix, amountGroups * 3, 2);
        // start rollout
        rolloutManagement.start(rollout.getId());

        // handleStartingRollout (no handleRunning called yet)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3);
        for (int i = 0; i < amountGroups; i++) {
            assertGroup(groups.get(i), false, i == 0 ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.SCHEDULED, 3);
        }
        assertGroup(dynamic1, true, RolloutGroupStatus.SCHEDULED, 0);

        executeStaticWithoutOneTargetFromTheLastGroup(groups, rollout, amountGroups);

        // partially fill the first dynamic (it is running and now create actions for 2 targets)
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 1, amountGroups * 3 + 2);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 2);

        // fill first and create second
        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 2, 2);
        rolloutHandler.handleAll(); // fill first dynamic group and create a new dynamic2
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        groups = rolloutGroupManagement.findByRollout(
                new OffsetBasedPageRequest(0, amountGroups + 10, Sort.by(Direction.ASC, "id")),
                rollout.getId()).getContent();
        final RolloutGroup dynamic2 = groups.get(amountGroups + 1);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);

        // create scheduled actions for the dynamic2
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.SCHEDULED, 0);
        assertAndGetRunning(rollout, 4); // one from the last static group and 3 from the first dynamic
        assertScheduled(rollout, 0);

        // executes last from static and dynamic1 without 1 target
        assertAndGetRunning(rollout, 4)// one from the last static and 3 for the first dynamic
                .stream()
                // remove the last assigned to dynamic1 - it could be amountGroups * 3 + 2 or bigger by id
                .filter(action -> Integer.parseInt(action.getTarget().getControllerId().substring(targetPrefix.length())) < amountGroups * 3 + 2)
                .forEach(this::finishAction);
        assertAndGetRunning(rollout, 1); // remains on in the first dynamic

        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 3);
        assertGroup(groups.get(amountGroups - 1), false, RolloutGroupStatus.FINISHED, 3);
        assertGroup(dynamic1, true, RolloutGroupStatus.RUNNING, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 0);

        rolloutHandler.handleAll(); // add 1 action to now running second dynamic
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 4);
        assertAndGetRunning(rollout, 2);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 1);

        testdataFactory.createTargets(targetPrefix, amountGroups * 3 + 4, 1);
        rolloutManagement.pauseRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.PAUSED, amountGroups + 2, amountGroups * 3 + 4);
        assertAndGetRunning(rollout, 2);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 1); // no new assignment

        rolloutManagement.resumeRollout(rollout.getId());
        rolloutHandler.handleAll();
        assertRollout(rollout, true, RolloutStatus.RUNNING, amountGroups + 2, amountGroups * 3 + 5);
        assertAndGetRunning(rollout, 3);
        assertGroup(dynamic2, true, RolloutGroupStatus.RUNNING, 2); // assign the target created when paused
    }

    private void executeStaticWithoutOneTargetFromTheLastGroup(
            final List<RolloutGroup> groups,
            final Rollout rollout, final int amountGroups) {
        // execute groups (without on of the last)
        assertThat(refresh(groups.get(0)).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        for (int i = 0; i < amountGroups; i++) {
            if (i + 1 < groups.size()) {
                assertThat(refresh(groups.get(i + 1)).getStatus()).isEqualTo(RolloutGroupStatus.SCHEDULED);
            }
            // skip on from the last group only
            final AtomicBoolean skipOne = new AtomicBoolean(i + 1 == amountGroups);
            assertAndGetRunning(rollout, 3)
                    .stream()
                    .filter(action -> {
                        if (skipOne.get()) {
                            skipOne.set(false);
                            // in the last group, skip first
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .forEach(this::finishAction);
            assertAndGetRunning(rollout, i + 1 == amountGroups ? 1 : 0);
            rolloutHandler.handleAll();
            final RolloutGroupStatus expectedStatus =
                    i + 1 == amountGroups ? RolloutGroupStatus.RUNNING : RolloutGroupStatus.FINISHED;
            assertThat(refresh(groups.get(i)).getStatus())
                    .as("Check that group %s is in status %s", i, expectedStatus)
                    .isEqualTo(expectedStatus);
            if (i + 1 != amountGroups) {
                assertThat(refresh(groups.get(i + 1)).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
            }
        }
    }
}