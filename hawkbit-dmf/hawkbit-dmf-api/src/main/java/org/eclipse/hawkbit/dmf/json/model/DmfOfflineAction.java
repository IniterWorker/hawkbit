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
    private final OfflineStatus status;
    private final Long distributionSetId;
    @JsonProperty
    private List<DmfActionUpdateStatus> statusEntries;

    public DmfOfflineAction(@JsonProperty(value = "status", required = true) final OfflineStatus status,
                            @JsonProperty(value = "distributionSetId", required = true) final Long distributionSetId) {
        this.status = status;
        this.distributionSetId = distributionSetId;
    }

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public OfflineStatus getStatus() {
        return status;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public List<DmfActionUpdateStatus> getStatusEntries() {
        return statusEntries;
    }

    public void setStatusEntries(List<DmfActionUpdateStatus> statusEntries) {
        this.statusEntries = statusEntries;
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public enum OfflineStatus {
        FINISHED,

        CANCELLED,

        ERROR
    }
}
