/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for Action consent POST
 *
 */
public class MgmtActionConsentPost {

    @JsonProperty(required = true)
    private Consent consent;

    @JsonProperty(required = true)
    private String userId;

    @JsonProperty(required = false)
    private String remark;

    @JsonIgnore
    private final LocalDateTime date;

    public MgmtActionConsentPost() {
        date = LocalDateTime.now();
    }

    public Consent getConsent() {
        return consent;
    }

    public void setConsent(final Consent consent) {
        this.consent = consent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * @return Remark why an action was confirmed or declined
     */
    public String getRemark() {
        return remark;
    }

    /**
     * @param remark
     *            Remark why an action was confirmed or declined
     */
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public enum Consent {
        PROVIDED("provided"), DENIED("denied");

        private final String state;

        Consent(final String state) {
            this.state = state;
        }
    }
}
