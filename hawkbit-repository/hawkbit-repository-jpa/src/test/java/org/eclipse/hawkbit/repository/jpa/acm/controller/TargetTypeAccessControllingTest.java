/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetTypeSpecification;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Component Tests - Access Control")
@Story("Test Target Type Access Controlling")
class TargetTypeAccessControllingTest extends AbstractAccessControllingTest {

    @Test
    @Description("Verifies read access rules for target types")
    void verifyTargetTypeReadOperations() {
        permitAllOperations(AccessController.Operation.CREATE);

        final TargetType permittedTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type1"));

        final TargetType hiddenTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type2"));

        // create target and assign with hidden target type
        final Target targetWithHiddenTargetType = targetManagement.create(entityFactory.target().create()
                .controllerId("targetWithUnseeableTargetType").targetType(hiddenTargetType.getId()));

        // create target and assign with permitted target type
        final Target targetWithPermittedTargetType = targetManagement.create(entityFactory.target().create()
                .controllerId("targetWithPermittedTargetType").targetType(permittedTargetType.getId()));

        // define access controlling rule
        testAccessControlManger.defineAccessRule(JpaTargetType.class, AccessController.Operation.READ,
                TargetTypeSpecification.hasId(permittedTargetType.getId()));

        // verify targetTypeManagement#findAll
        assertThat(targetTypeManagement.findAll(Pageable.unpaged()).get().map(Identifiable::getId).toList())
                .containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByRsql
        assertThat(targetTypeManagement.findByRsql(Pageable.unpaged(), "id==*").get().map(Identifiable::getId).toList())
                .containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByTargetControllerId
        assertThat(targetTypeManagement.findByTargetControllerId(targetWithPermittedTargetType.getControllerId()))
                .hasValueSatisfying(foundType -> assertThat(foundType.getId()).isEqualTo(permittedTargetType.getId()));
        assertThat(targetTypeManagement.findByTargetControllerId(targetWithHiddenTargetType.getControllerId()))
                .isEmpty();

        // verify targetTypeManagement#findByTargetControllerIds
        assertThat(
                targetTypeManagement
                        .findByTargetControllerIds(Arrays.asList(targetWithPermittedTargetType.getControllerId(),
                                targetWithHiddenTargetType.getControllerId()))
                        .stream().map(Identifiable::getId).toList())
                .hasSize(1).containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByTargetId
        assertThat(targetTypeManagement.findByTargetId(targetWithPermittedTargetType.getId()))
                .hasValueSatisfying(foundType -> assertThat(foundType.getId()).isEqualTo(permittedTargetType.getId()));
        assertThat(targetTypeManagement.findByTargetId(targetWithHiddenTargetType.getId())).isEmpty();

        // verify targetTypeManagement#findByTargetIds
        assertThat(targetTypeManagement
                .findByTargetIds(
                        Arrays.asList(targetWithPermittedTargetType.getId(), targetWithHiddenTargetType.getId()))
                .stream().map(Identifiable::getId).toList()).hasSize(1).containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#findByName
        assertThat(targetTypeManagement.findByName(Pageable.unpaged(), permittedTargetType.getName()).getContent())
                .hasSize(1).satisfies(results -> {
                    assertThat(results.get(0).getId()).isEqualTo(permittedTargetType.getId());
                });
        assertThat(targetTypeManagement.findByName(Pageable.unpaged(), hiddenTargetType.getName())).isEmpty();

        // verify targetTypeManagement#count
        assertThat(targetTypeManagement.count()).isEqualTo(1);

        // verify targetTypeManagement#countByName
        assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
        assertThat(targetTypeManagement.countByName(hiddenTargetType.getName())).isZero();

        // verify targetTypeManagement#countByName
        assertThat(targetTypeManagement.countByName(permittedTargetType.getName())).isEqualTo(1);
        assertThat(targetTypeManagement.countByName(hiddenTargetType.getName())).isZero();

        // verify targetTypeManagement#get by id
        assertThat(targetTypeManagement.get(permittedTargetType.getId())).isPresent();
        assertThat(targetTypeManagement.get(hiddenTargetType.getId())).isEmpty();

        // verify targetTypeManagement#getByName
        assertThat(targetTypeManagement.getByName(permittedTargetType.getName())).isPresent();
        assertThat(targetTypeManagement.getByName(hiddenTargetType.getName())).isEmpty();

        // verify targetTypeManagement#get by ids
        assertThat(targetTypeManagement.get(Arrays.asList(permittedTargetType.getId(), hiddenTargetType.getId()))
                .stream().map(Identifiable::getId).toList()).containsOnly(permittedTargetType.getId());

        // verify targetTypeManagement#update is not possible. Assert exception thrown.
        assertThatThrownBy(() -> targetTypeManagement.update(entityFactory.targetType().update(hiddenTargetType.getId())
                .name(hiddenTargetType.getName() + "/new").description("newDesc")))
                .as("Target type update shouldn't be allowed since the target type is not visible.")
                .isInstanceOf(EntityNotFoundException.class);

        // verify targetTypeManagement#delete is not possible. Assert exception thrown.
        assertThatThrownBy(() -> targetTypeManagement.delete(hiddenTargetType.getId()))
                .as("Target type delete shouldn't be allowed since the target type is not visible.")
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @Description("Verifies delete access rules for target types")
    void verifyTargetTypeDeleteOperations() {
        permitAllOperations(AccessController.Operation.CREATE);
        final TargetType manageableTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type1"));

        final TargetType readOnlyTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type2"));

        // define access controlling rule to allow reading both types
        testAccessControlManger.defineAccessRule(JpaTargetType.class, AccessController.Operation.READ,
                TargetTypeSpecification
                        .hasIdIn(Arrays.asList(manageableTargetType.getId(), readOnlyTargetType.getId())));

        // permit operation to delete permittedTargetType
        testAccessControlManger.permitOperation(JpaTargetType.class, AccessController.Operation.DELETE,
                type -> type.getId().equals(manageableTargetType.getId()));

        // delete the manageableTargetType
        targetTypeManagement.delete(manageableTargetType.getId());

        // verify targetTypeManagement#delete for readOnlyTargetType is not possible
        assertThatThrownBy(() -> {
            targetTypeManagement.delete(readOnlyTargetType.getId());
        }).isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    @Description("Verifies update operation for target types")
    void verifyTargetTypeUpdateOperations() {
        permitAllOperations(AccessController.Operation.CREATE);
        final TargetType manageableTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type1"));

        final TargetType readOnlyTargetType = targetTypeManagement
                .create(entityFactory.targetType().create().name("type2"));

        // define access controlling rule to allow reading both types
        testAccessControlManger.defineAccessRule(JpaTargetType.class, AccessController.Operation.READ,
                TargetTypeSpecification
                        .hasIdIn(Arrays.asList(manageableTargetType.getId(), readOnlyTargetType.getId())));

        // permit updating the manageableTargetType
        testAccessControlManger.permitOperation(JpaTargetType.class, AccessController.Operation.UPDATE,
                type -> type.getId().equals(manageableTargetType.getId()));

        // update the manageableTargetType
        targetTypeManagement.update(entityFactory.targetType().update(manageableTargetType.getId())
                .name(manageableTargetType.getName() + "/new").description("newDesc"));

        // verify targetTypeManagement#update for readOnlyTargetType is not possible
        assertThatThrownBy(() -> {
            targetTypeManagement.update(entityFactory.targetType().update(readOnlyTargetType.getId())
                    .name(readOnlyTargetType.getName() + "/new").description("newDesc"));
        }).isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    @Description("Verifies create operation blocked by controller")
    void verifyTargetTypeCreationBlockedByAccessController() {
        // verify targetTypeManagement#create for any type
        assertThatThrownBy(() -> targetTypeManagement.create(entityFactory.targetType().create().name("type1")))
                .as("Target type create shouldn't be allowed since the target type is not visible.")
                .isInstanceOf(InsufficientPermissionException.class);
    }

}
