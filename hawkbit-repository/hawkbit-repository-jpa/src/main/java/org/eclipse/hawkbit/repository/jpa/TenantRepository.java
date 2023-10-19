/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaTenant;
import org.eclipse.hawkbit.repository.model.Tenant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * repository for operations on {@link Tenant} entity.
 *
 */
@Transactional(readOnly = true)
public interface TenantRepository
        extends PagingAndSortingRepository<JpaTenant, Long>,
        CrudRepository<JpaTenant, Long> {

    /**
     * Search {@link Tenant} by tenant name.
     *
     * @param tenant
     *            to search for
     * @return found {@link Tenant} or <code>null</code>
     */
    Tenant findByTenantIgnoreCase(String tenant);

    @Override
    List<JpaTenant> findAll();

    /**
     * @param tenant
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenant t WHERE UPPER(t.tenant) = UPPER(:tenant)")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);

}
