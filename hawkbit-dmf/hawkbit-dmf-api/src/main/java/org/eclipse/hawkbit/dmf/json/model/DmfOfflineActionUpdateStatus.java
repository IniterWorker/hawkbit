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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfOfflineActionUpdateStatus {

    private final DmfActionStatus actionStatus;

    @JsonProperty
    private List<String> message;

    @JsonProperty
    private Integer code;

    public DmfOfflineActionUpdateStatus(@JsonProperty(value = "actionStatus", required = true) final DmfActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }

    public List<String> getMessage() {
        if (message == null) {
            return Collections.emptyList();
        }

        return message;
    }

    public boolean addMessage(final String message) {
        if (this.message == null) {
            this.message = new ArrayList<>();
        }

        return this.message.add(message);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public DmfActionStatus getActionStatus() {
        return actionStatus;
    }
}
