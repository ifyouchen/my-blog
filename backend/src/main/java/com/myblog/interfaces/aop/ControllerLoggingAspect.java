//package com.myblog.interfaces.aop;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import javax.servlet.http.HttpServletRequest;
//
//@Aspect
//@Component
//public class ControllerLoggingAspect {
//
//    private static final Logger log = LoggerFactory.getLogger(ControllerLoggingAspect.class);
//
//    @Pointcut("execution(* com.myblog.interfaces.rest.controller..*.*(..))")
//    public void controllerMethods() {}
//
//    @Around("controllerMethods()")
//    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
//        long startTime = System.nanoTime();
//
//        HttpServletRequest request = getCurrentRequest();
//        String methodName = joinPoint.getSignature().toShortString();
//        String requestUri = request != null ? request.getRequestURI() : "unknown";
//        String httpMethod = request != null ? request.getMethod() : "unknown";
//        String traceId = MDC.get("traceId");
//        String params = buildParamsString(joinPoint.getArgs());
//
//        log.info("[CTRL] [traceId={}] [HTTP {}] {} - Method: {} - Args: {}",
//            traceId, httpMethod, requestUri, methodName, params);
//
//        Throwable error = null;
//
//        try {
//            return joinPoint.proceed();
//        } catch (Throwable t) {
//            error = t;
//            throw t;
//        } finally {
//            double durationMs = (System.nanoTime() - startTime) / 1_000_000.0D;
//            if (error != null) {
//                log.error("[CTRL] [traceId={}] [HTTP {}] {} - Method: {} - Business: {}ms - Error: {}",
//                    traceId, httpMethod, requestUri, methodName, formatDuration(durationMs), error.getMessage());
//            } else {
//                log.info("[CTRL] [traceId={}] [HTTP {}] {} - Method: {} - Business: {}ms - Success",
//                    traceId, httpMethod, requestUri, methodName, formatDuration(durationMs));
//            }
//        }
//    }
//
//    private HttpServletRequest getCurrentRequest() {
//        try {
//            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            return attrs != null ? attrs.getRequest() : null;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    private String buildParamsString(Object[] args) {
//        if (args == null || args.length == 0) {
//            return "{}";
//        }
//        StringBuilder sb = new StringBuilder("{");
//        for (int i = 0; i < args.length; i++) {
//            if (i > 0) sb.append(", ");
//            Object arg = args[i];
//            if (arg == null) {
//                sb.append("null");
//            } else if (isSimpleType(arg)) {
//                sb.append(arg);
//            } else {
//                sb.append(arg.getClass().getSimpleName());
//            }
//        }
//        sb.append("}");
//        return sb.toString();
//    }
//
//    private boolean isSimpleType(Object obj) {
//        return obj instanceof String ||
//               obj instanceof Number ||
//               obj instanceof Boolean;
//    }
//
//    private String formatDuration(double durationMs) {
//        return String.format("%.2f", durationMs);
//    }
//}
