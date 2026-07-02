package com.cms.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sliding-window rate limiter: 300 requests per IP per minute.
 * Runs after Spring Security so only authenticated traffic is counted.
 * Protects against scripted abuse using a stolen token.
 */
@Component
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_REQUESTS_PER_WINDOW = 300;
    private static final long WINDOW_MS               = 60_000L;

    // [requestCount, windowStartMs]
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String ip = resolveClientIp(request);

        long[] bucket = buckets.compute(ip, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing[1] >= WINDOW_MS) {
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });

        if (bucket[0] > MAX_REQUESTS_PER_WINDOW) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please slow down.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Hourly cleanup of stale buckets to prevent memory growth
    @Scheduled(fixedRate = 3_600_000L)
    public void evictStaleBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue()[1] >= WINDOW_MS);
    }
}
