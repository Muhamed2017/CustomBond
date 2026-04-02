package com.custombond.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.custombond.service..*) || within(com.custombond.controller..*)")
    public void applicationPackagePointcut() {}

    @Around("applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("→ {}.{}()", className, methodName);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("← {}.{}() completed in {}ms", className, methodName, elapsed);
            return result;
        } catch (Exception ex) {
            log.error("✗ {}.{}() threw: {}", className, methodName, ex.getMessage());
            throw ex;
        }
    }

}
