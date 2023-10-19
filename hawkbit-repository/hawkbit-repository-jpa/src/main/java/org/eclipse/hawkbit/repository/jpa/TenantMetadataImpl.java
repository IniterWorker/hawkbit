/*
 * *
 *  * Copyright (c) 2023 Bosch.IO GmbH and others.
 *  *
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Public License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Tenant;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;

public class TenantMetadataImpl implements TenantMetaData {

  private final Tenant                        tenant;
  private final TenantConfigurationRepository tenantConfigurationRepository;
  private final DistributionSetTypeRepository distributionSetTypeRepository;

  public TenantMetadataImpl(Tenant tenant, TenantConfigurationRepository tenantConfigurationRepository,
      DistributionSetTypeRepository distributionSetTypeRepository) {
    this.tenant = tenant;
    this.tenantConfigurationRepository = tenantConfigurationRepository;
    this.distributionSetTypeRepository = distributionSetTypeRepository;
  }

  @Override
  public String getTenant() {
    return tenant.getTenant();
  }

  @Override
  public Long getId() {
    return tenant.getId();
  }

  @Override
  public DistributionSetType getDefaultDsType() {
    //TODO check if could be optimized as a result it would be always requested whenever called
    JpaTenantConfiguration byKey = tenantConfigurationRepository.findByKey(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_DISTRIBUTION_SET_TYPE);
    String                 value = byKey.getValue();
    return distributionSetTypeRepository.findById(Long.valueOf(value)).get();
  }

  public void setDefaultDsType(DistributionSetType distributionSetType) {
    JpaTenantConfiguration byKey = tenantConfigurationRepository.findByKey(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_DISTRIBUTION_SET_TYPE);
    byKey.setValue(String.valueOf(distributionSetType.getId()));
    tenantConfigurationRepository.save(byKey);
  }
}
