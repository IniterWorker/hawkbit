/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Thrown if user tries to assign a {@link DistributionSet} to a {@link Target}
 * that has an incompatible {@link TargetType}
 */
public class IncompatibleTargetTypeException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private String targetTypeName;
    private String distributionSetTypeName;

    /**
     * Creates a new IncompatibleTargetTypeException with
     * {@link SpServerError#SP_TARGET_TYPE_INCOMPATIBLE} error.
     */
    public IncompatibleTargetTypeException() {
        super(SpServerError.SP_TARGET_TYPE_INCOMPATIBLE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public IncompatibleTargetTypeException(final Throwable cause) {
        super(SpServerError.SP_TARGET_TYPE_INCOMPATIBLE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public IncompatibleTargetTypeException(final String message) {
        super(message, SpServerError.SP_TARGET_TYPE_INCOMPATIBLE);
    }

    /**
     * Constructor with more specific error message
     * 
     * @param targetTypeName
     *            Name of the target type
     * @param distributionSetTypeName
     *            Name of the distribution set type
     */
    public IncompatibleTargetTypeException(final String targetTypeName, final String distributionSetTypeName) {
        this(String.format("Target of type [%s] is not compatible with distribution set of type [%s]", targetTypeName,
                distributionSetTypeName));
        this.targetTypeName = targetTypeName;
        this.distributionSetTypeName = distributionSetTypeName;
    }

    public String getTargetTypeName() {
        return targetTypeName;
    }

    public String getDistributionSetTypeName() {
        return distributionSetTypeName;
    }
}
