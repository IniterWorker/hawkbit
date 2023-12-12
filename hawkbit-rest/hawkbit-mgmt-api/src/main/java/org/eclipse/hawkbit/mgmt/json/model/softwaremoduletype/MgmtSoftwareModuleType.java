/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.hawkbit.mgmt.json.model.MgmtTypeEntity;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleType extends MgmtTypeEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "83")
    private Long moduleId;

    @JsonProperty
    @Schema(example = "1")
    private int maxAssignments;

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }

    public void setMaxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }
}
