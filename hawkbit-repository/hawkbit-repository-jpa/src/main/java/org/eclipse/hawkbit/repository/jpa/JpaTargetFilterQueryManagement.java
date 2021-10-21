/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericTargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetFilterQuerySpecification;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

import cz.jirutka.rsql.parser.RSQLParserException;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetFilterQueryManagement implements TargetFilterQueryManagement {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaTargetFilterQueryManagement.class);

    private static final int TARGET_FILTER_PAGE_LIMIT = 50;

    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final TargetManagement targetManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final DistributionSetManagement distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantAware tenantAware;
    private final PlatformTransactionManager txManager;
    private final LockRegistry lockRegistry;
    private final AutoAssignExecutor autoAssignExecutor;

    private final Database database;

    JpaTargetFilterQueryManagement(final TargetFilterQueryRepository targetFilterQueryRepository,
            final TargetManagement targetManagement, final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final Database database, final TenantConfigurationManagement tenantConfigurationManagement,
            final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware,
            final PlatformTransactionManager txManager, final LockRegistry lockRegistry,
            final AutoAssignExecutor autoAssignExecutor) {
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetManagement = targetManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.database = database;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantAware = tenantAware;
        this.txManager = txManager;
        this.lockRegistry = lockRegistry;
        this.autoAssignExecutor = autoAssignExecutor;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetFilterQuery create(final TargetFilterQueryCreate c) {
        final JpaTargetFilterQueryCreate create = (JpaTargetFilterQueryCreate) c;

        create.getQuery().ifPresent(query -> {
            // validate the RSQL query syntax
            RSQLUtility.validateRsqlFor(query, TargetFields.class);

            // enforce the 'max targets per auto assign' quota right here even
            // if the result of the filter query can vary over time
            if (create.getAutoAssignDistributionSetId().isPresent()) {
                WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                        .validate(create);
                assertMaxTargetsQuota(query);
            }
        });

        return targetFilterQueryRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long targetFilterQueryId) {
        if (!targetFilterQueryRepository.existsById(targetFilterQueryId)) {
            throw new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId);
        }

        targetFilterQueryRepository.deleteById(targetFilterQueryId);
    }

    @Override
    public Page<TargetFilterQuery> findAll(final Pageable pageable) {
        return convertPage(targetFilterQueryRepository.findAll(pageable), pageable);
    }

    @Override
    public long count() {
        return targetFilterQueryRepository.count();
    }

    @Override
    public long countByAutoAssignDistributionSetId(final long autoAssignDistributionSetId) {
        return targetFilterQueryRepository.countByAutoAssignDistributionSetId(autoAssignDistributionSetId);
    }

    private static Page<TargetFilterQuery> convertPage(final Page<JpaTargetFilterQuery> findAll,
            final Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<TargetFilterQuery> findByName(final Pageable pageable, final String name) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(name)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.likeName(name));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByRsql(final Pageable pageable, final String rsqlFilter) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(rsqlFilter)) {
            specList = Collections.singletonList(RSQLUtility.buildRsqlSpecification(rsqlFilter,
                    TargetFilterQueryFields.class, virtualPropertyReplacer, database));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByQuery(final Pageable pageable, final String query) {
        List<Specification<JpaTargetFilterQuery>> specList = Collections.emptyList();
        if (!StringUtils.isEmpty(query)) {
            specList = Collections.singletonList(TargetFilterQuerySpecification.equalsQuery(query));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findByAutoAssignDSAndRsql(final Pageable pageable, final long setId,
            final String rsqlFilter) {
        final List<Specification<JpaTargetFilterQuery>> specList = Lists.newArrayListWithExpectedSize(2);

        final DistributionSet distributionSet = distributionSetManagement.getOrElseThrowException(setId);

        specList.add(TargetFilterQuerySpecification.byAutoAssignDS(distributionSet));

        if (!StringUtils.isEmpty(rsqlFilter)) {
            specList.add(RSQLUtility.buildRsqlSpecification(rsqlFilter, TargetFilterQueryFields.class,
                    virtualPropertyReplacer, database));
        }
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Page<TargetFilterQuery> findWithAutoAssignDS(final Pageable pageable) {
        final List<Specification<JpaTargetFilterQuery>> specList = Collections
                .singletonList(TargetFilterQuerySpecification.withAutoAssignDS());
        return convertPage(findTargetFilterQueryByCriteriaAPI(pageable, specList), pageable);
    }

    private Page<JpaTargetFilterQuery> findTargetFilterQueryByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaTargetFilterQuery>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return targetFilterQueryRepository.findAll(pageable);
        }

        final Specification<JpaTargetFilterQuery> specs = SpecificationsBuilder.combineWithAnd(specList);
        return targetFilterQueryRepository.findAll(specs, pageable);
    }

    @Override
    public Optional<TargetFilterQuery> getByName(final String targetFilterQueryName) {
        return targetFilterQueryRepository.findByName(targetFilterQueryName);
    }

    @Override
    public Optional<TargetFilterQuery> get(final long targetFilterQueryId) {
        return targetFilterQueryRepository.findById(targetFilterQueryId).map(tfq -> (TargetFilterQuery) tfq);
    }

    @Override
    @Transactional
    public TargetFilterQuery update(final TargetFilterQueryUpdate u) {
        final GenericTargetFilterQueryUpdate update = (GenericTargetFilterQueryUpdate) u;

        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(update.getId());

        update.getName().ifPresent(targetFilterQuery::setName);
        update.getQuery().ifPresent(query -> {

            // enforce the 'max targets per auto assignment'-quota only if the
            // query is going to change
            if (targetFilterQuery.getAutoAssignDistributionSet() != null
                    && !query.equals(targetFilterQuery.getQuery())) {
                assertMaxTargetsQuota(query);
            }

            // set the new query
            targetFilterQuery.setQuery(query);
        });

        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    @Override
    @Transactional
    public TargetFilterQuery updateAutoAssignDS(final AutoAssignDistributionSetUpdate update) {
        final JpaTargetFilterQuery targetFilterQuery = findTargetFilterQueryOrThrowExceptionIfNotFound(
                update.getTargetFilterId());
        if (update.getDsId() == null) {
            targetFilterQuery.setAutoAssignDistributionSet(null);
            targetFilterQuery.setAutoAssignActionType(null);
            targetFilterQuery.setAutoAssignWeight(null);
            targetFilterQuery.setAutoAssignInitiatedBy(null);
        } else {
            WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).validate(update);
            // we cannot be sure that the quota was enforced at creation time
            // because the Target Filter Query REST API does not allow to
            // specify an
            // auto-assign distribution set when creating a target filter query
            assertMaxTargetsQuota(targetFilterQuery.getQuery());
            final JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                    .getValidAndComplete(update.getDsId());
            verifyDistributionSetAndThrowExceptionIfDeleted(ds);
            targetFilterQuery.setAutoAssignDistributionSet(ds);
            targetFilterQuery.setAutoAssignInitiatedBy(tenantAware.getCurrentUsername());
            targetFilterQuery.setAutoAssignActionType(sanitizeAutoAssignActionType(update.getActionType()));
            targetFilterQuery.setAutoAssignWeight(update.getWeight());
        }
        return targetFilterQueryRepository.save(targetFilterQuery);
    }

    private static void verifyDistributionSetAndThrowExceptionIfDeleted(final DistributionSet distributionSet) {
        if (distributionSet.isDeleted()) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSet.getId());
        }
    }

    private static ActionType sanitizeAutoAssignActionType(final ActionType actionType) {
        if (actionType == null) {
            return ActionType.FORCED;
        }

        if (!TargetFilterQuery.ALLOWED_AUTO_ASSIGN_ACTION_TYPES.contains(actionType)) {
            throw new InvalidAutoAssignActionTypeException();
        }

        return actionType;
    }

    private JpaTargetFilterQuery findTargetFilterQueryOrThrowExceptionIfNotFound(final Long queryId) {
        return targetFilterQueryRepository.findById(queryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, queryId));
    }

    @Override
    public boolean verifyTargetFilterQuerySyntax(final String query) {
        try {
            RSQLUtility.validateRsqlFor(query, TargetFields.class);
            return true;
        } catch (RSQLParserException | RSQLParameterUnsupportedFieldException e) {
            LOGGER.debug("The RSQL query '" + query + "' is invalid.", e);
            return false;
        }
    }

    private void assertMaxTargetsQuota(final String query) {
        QuotaHelper.assertAssignmentQuota(targetManagement.countByRsql(query),
                quotaManagement.getMaxTargetsPerAutoAssignment(), Target.class, TargetFilterQuery.class);
    }

    @Override
    @Transactional
    public void cancelAutoAssignmentForDistributionSet(final long setId) {
        targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionType(setId);
        LOGGER.debug("Auto assignments for distribution sets {} deactivated", setId);
    }

    @Override
    // No transaction, will be created per handled target filter
    @Transactional(propagation = Propagation.NEVER)
    public void handleAutoAssignments() {
        final String tenant = tenantAware.getCurrentTenant();
        final String handlerId = createAutoassignLockKey(tenant);
        final Lock lock = lockRegistry.obtain(handlerId);
        if (!lock.tryLock()) {
            return;
        }

        try {
            Page<JpaTargetFilterQuery> filterQueries;
            Pageable page = PageRequest.of(0, TARGET_FILTER_PAGE_LIMIT);
            do {
                filterQueries = targetFilterQueryRepository.findAll(TargetFilterQuerySpecification.withAutoAssignDS(),
                        page);
                filterQueries.forEach(filterQuery -> handleAutoAssignment(handlerId, filterQuery));
            } while ((page = filterQueries.nextPageable()) != Pageable.unpaged());
        } finally {
            lock.unlock();
        }
    }

    public static String createAutoassignLockKey(final String tenant) {
        return tenant + "-autoassignment";
    }

    private void handleAutoAssignment(final String transactionNamePrefix, final TargetFilterQuery filterQuery) {
        final String transactionId = transactionNamePrefix + "-" + filterQuery.getId();
        try {
            DeploymentHelper.runInNewTransaction(txManager, transactionId, status -> {
                runInUserContext(filterQuery, () -> autoAssignExecutor.execute(filterQuery));
                return null;
            });
        } catch (final RuntimeException ex) {
            LOGGER.debug(
                    "Exception on forEachFilterWithAutoAssignDS execution for tenant {} with filter id {}, transaction id {}. Continue with next filter query.",
                    filterQuery.getTenant(), filterQuery.getId(), transactionId, ex);
            LOGGER.error(
                    "Exception on forEachFilterWithAutoAssignDS execution for tenant {} with filter id {}, transaction id {} and error message [{}]. "
                            + "Continue with next filter query.",
                    filterQuery.getTenant(), filterQuery.getId(), transactionId, ex.getMessage());
        }
    }

    private void runInUserContext(final TargetFilterQuery targetFilterQuery, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler,
                () -> Objects.requireNonNull(getAutoAssignmentInitiatedBy(targetFilterQuery)), tenantAware);
    }

    private static String getAutoAssignmentInitiatedBy(final TargetFilterQuery targetFilterQuery) {
        return StringUtils.isEmpty(targetFilterQuery.getAutoAssignInitiatedBy()) ? targetFilterQuery.getCreatedBy()
                : targetFilterQuery.getAutoAssignInitiatedBy();
    }
}
