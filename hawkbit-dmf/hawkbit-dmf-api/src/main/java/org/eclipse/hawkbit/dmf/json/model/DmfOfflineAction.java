/***
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfOfflineAction {

    @JsonProperty
    private String externalRef; // leave it optional for now, it is not mandatory for us ?
    private final DmfActionStatus status;
    private final Long distributionSetId;

    private final String controllerId;
    @JsonProperty
    private List<DmfOfflineActionUpdateStatus> statusEntries;

    public DmfOfflineAction(@JsonProperty(value = "status", required = true) final DmfActionStatus status,
                            @JsonProperty(value = "distributionSetId", required = true) final Long distributionSetId,
                            @JsonProperty(value = "controllerId", required = true) final String controllerId) {
        this.status = status;
        this.distributionSetId = distributionSetId;
        this.controllerId = controllerId;
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public DmfActionStatus getStatus() {
        return status;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public List<DmfOfflineActionUpdateStatus> getStatusEntries() {
        return statusEntries;
    }

    public void setStatusEntries(List<DmfOfflineActionUpdateStatus> statusEntries) {
        this.statusEntries = statusEntries;
    }

    public String getControllerId() {
        return controllerId;
    }
}
