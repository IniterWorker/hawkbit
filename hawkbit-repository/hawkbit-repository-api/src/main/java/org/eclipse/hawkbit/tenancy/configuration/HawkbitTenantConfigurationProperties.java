/**
 * Copyright (c) 2022 Bosch.IO, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.tenancy.configuration;

import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Properties for tenant configuration default values.
 *
 */
@ConfigurationProperties("hawkbit.server.tenant")
public class HawkbitTenantConfigurationProperties implements TenantConfigurationProperties {

    private final Map<String, TenantConfigurationKey> configuration = new HashMap<>();

    /**
     * @return full map of all configured tenant properties
     */
    @Override
    public Map<String, TenantConfigurationKey> getConfiguration() {
        return configuration;
    }

    /**
     * @return full list of {@link TenantConfigurationKey}s
     */
    @Override
    public Collection<TenantConfigurationKey> getConfigurationKeys() {
        return configuration.values();
    }

    /**
     * @param keyName
     *            name of the TenantConfigurationKey
     * @return the TenantConfigurationKey with the name keyName
     */
    @Override
    public TenantConfigurationKey fromKeyName(final String keyName) {
        return configuration.values().stream().filter(conf -> conf.getKeyName().equals(keyName)).findAny()
                .orElseThrow(() -> new InvalidTenantConfigurationKeyException(
                        "The given configuration key " + keyName + " does not exist."));
    }

}
