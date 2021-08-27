package org.eclipse.hawkbit.repository.jpa.factory;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;

import javax.persistence.EntityManager;

public class AclRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends JpaRepositoryFactoryBean<T, S, ID> {
    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public AclRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(@NonNull final EntityManager entityManager) {
        RepositoryFactorySupport rfs = super.createRepositoryFactory(entityManager);
        rfs.addRepositoryProxyPostProcessor(new AclInjectionPostProcessor());
        return rfs;
    }
}
