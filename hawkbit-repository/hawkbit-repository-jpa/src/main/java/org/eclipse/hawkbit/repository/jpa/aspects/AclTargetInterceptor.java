package org.eclipse.hawkbit.repository.jpa.aspects;

import java.util.Arrays;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.springframework.data.jpa.domain.Specification;

public class AclTargetInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        final Object[] arguments = methodInvocation.getArguments();
        for(int i=0; i<arguments.length; i++){
          if (arguments[i] instanceof Specification){
              Specification spec = (Specification) arguments[i];
              arguments[i] = TargetSpecifications.hasIdIn(Arrays.asList(1L, 2L, 3L)).and(spec);
              break;
          }
        }
        return methodInvocation.proceed();
    }
}
