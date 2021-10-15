/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for Action consent POST
 *
 */
public class MgmtActionConfirmOrDeclineRemark {

    @JsonProperty(required = true)
    private String remark;

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

}
