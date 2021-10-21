/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.autoassign;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * An interface declaration which contains the check for the auto assignment
 * logic.
 */
@FunctionalInterface
public interface AutoAssignExecutor {

    /**
     * Executes assignment for provided {@link TargetFilterQuery} with
     * autoassign {@link DistributionSet}
     */
    void execute(final TargetFilterQuery targetFilterQuery);

}
