package org.eclipse.hawkbit.repository.jpa.factory;

import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.aspects.AclTargetInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;


public class AclInjectionPostProcessor implements RepositoryProxyPostProcessor {
    @Override
    public void postProcess(final ProxyFactory proxyFactory,
            final RepositoryInformation repositoryInformation) {
        // Only inject advice into TargetRepository
        if(repositoryInformation.getRepositoryInterface().equals(TargetRepository.class)) {
            proxyFactory.addAdvice(new AclTargetInterceptor());
        }
    }
}
