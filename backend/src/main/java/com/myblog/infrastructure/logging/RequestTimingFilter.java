package com.myblog.infrastructure.logging;

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
 * 请求级 traceId 过滤器。
 *
 * <p>为每个请求生成或透传 traceId 并设置 MDC，使得业务日志自动携带 traceId。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 优先透传上游链路的 traceId，缺失时生成新的短 ID。
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null) {
            traceId = traceId.trim();
        }
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }
        // 12 位足够区分请求，同时比完整 UUID 更适合日志排查时人工检索。
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
