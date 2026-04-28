package com.myblog.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 请求级耗时日志过滤器。
 *
 * <p>负责为每个请求生成或透传 traceId，并记录请求总耗时。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        long startNs = System.nanoTime();
        String method = request.getMethod();
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String requestTarget = queryString == null || queryString.trim().isEmpty()
            ? requestUri
            : requestUri + "?" + queryString;

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        log.info("[REQ] [traceId={}] {} {}", traceId, method, requestTarget);

        try {
            filterChain.doFilter(request, response);
        } finally {
            double totalMs = (System.nanoTime() - startNs) / 1_000_000.0D;
            log.info("[RES] [traceId={}] {} {} - Status: {} - Total: {}ms",
                traceId, method, requestTarget, response.getStatus(), formatDuration(totalMs));
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null) {
            traceId = traceId.trim();
        }
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String formatDuration(double durationMs) {
        return String.format("%.2f", durationMs);
    }
}
