/*
 * Copyright 2015 PayU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.payu.ratel.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ProxyMethodInvocation;

public class RetryPolicyInvocationHandler implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryPolicyInvocationHandler.class);

    private static final long RETRY_TIME = 5000;

    private static final long RETRY_COUNT = 5;

    private final Class exception;

    public RetryPolicyInvocationHandler(Class exception) {
        this.exception = exception;
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Object invokeWithRetry(MethodInvocation methodInvocation, int count) throws Throwable {
        MethodInvocation methodInvocationCopy = ((ProxyMethodInvocation) methodInvocation).invocableClone();
        try {
            return methodInvocationCopy.proceed();
        } catch (Throwable thrownException) {
            LOGGER.info("Service thrown exception");
            if (isInStacktrace(thrownException, exception) && count < RETRY_COUNT) {
                Thread.sleep(RETRY_TIME);

                return invokeWithRetry(methodInvocation, count + 1);
            }

            throw thrownException;
        }
    }

    private boolean isInStacktrace(Throwable stackTrace, Class target) {
        Throwable t = stackTrace;
        while (t != null) {
            if (t.getClass().equals(target)) {
                return true;
            }

            t = t.getCause();
        }

        return false;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        return invokeWithRetry(methodInvocation, 1);
    }
}
