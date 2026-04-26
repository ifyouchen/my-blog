package com.myblog.interfaces.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class ControllerLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Pointcut("execution(* com.myblog.interfaces.rest.controller..*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get request info
        HttpServletRequest request = getCurrentRequest();
        String methodName = joinPoint.getSignature().toShortString();
        String requestUri = request != null ? request.getRequestURI() : "unknown";
        String httpMethod = request != null ? request.getMethod() : "unknown";

        // Build params string
        String params = buildParamsString(joinPoint.getArgs());

        // Log request
        log.info("[HTTP {}] {} - Args: {}", httpMethod, requestUri, params);

        Object result = null;
        Throwable error = null;
        long duration = 0;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            duration = System.currentTimeMillis() - startTime;
            if (error != null) {
                log.error("[HTTP {}] {} - Duration: {}ms - Error: {}",
                    httpMethod, requestUri, duration, error.getMessage());
            } else {
                log.info("[HTTP {}] {} - Duration: {}ms - Success",
                    httpMethod, requestUri, duration);
            }
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildParamsString(Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (isSimpleType(arg)) {
                sb.append(arg);
            } else {
                sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private boolean isSimpleType(Object obj) {
        return obj instanceof String ||
               obj instanceof Number ||
               obj instanceof Boolean;
    }
}