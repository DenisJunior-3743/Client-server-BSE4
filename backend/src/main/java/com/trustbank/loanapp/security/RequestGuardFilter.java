package com.trustbank.loanapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.common.ApiError;
import com.trustbank.loanapp.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protection duty (Comer 2.3.2): the server must not let one client exhaust it.
 *
 * <ul>
 *   <li>HTTPS only when {@code app.security.require-https} is on.</li>
 *   <li>Payload limit: bodies larger than {@code max-json-body-bytes} are refused with 413.</li>
 *   <li>Rate limit: a fixed one-minute window per client address, stricter on /auth (password and
 *       OTP guessing).</li>
 * </ul>
 *
 * <p>This is the one place the server keeps shared mutable memory, and it is deliberate: the
 * counters are soft state held in thread-safe structures ({@link ConcurrentHashMap},
 * {@link AtomicInteger}). Losing them on a restart only resets the windows; no business fact lives
 * here, so the server is still stateless in Comer's sense.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestGuardFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AppProperties.Security props;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RequestGuardFilter(AppProperties appProperties, ObjectMapper objectMapper, Clock clock) {
        this.props = appProperties.security();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (props.requireHttps() && !request.isSecure()) {
            reject(response, request, 403, "HTTPS_REQUIRED", "This API is only served over HTTPS");
            return;
        }
        if (request.getContentLengthLong() > props.maxJsonBodyBytes()) {
            reject(response, request, 413, "PAYLOAD_TOO_LARGE", "Request body is too large");
            return;
        }

        boolean authPath = request.getRequestURI().startsWith("/api/v1/auth/");
        int limit = authPath ? props.rateLimit().authRequestsPerMinute() : props.rateLimit().requestsPerMinute();
        long now = clock.millis();
        long windowStart = now - (now % WINDOW_MILLIS);
        String bucket = (authPath ? "auth:" : "api:") + request.getRemoteAddr();

        Window window = windows.compute(bucket,
                (k, existing) -> existing == null || existing.start != windowStart ? new Window(windowStart) : existing);
        if (window.count.incrementAndGet() > limit) {
            long retryAfter = Math.max(1, (windowStart + WINDOW_MILLIS - now) / 1000);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            reject(response, request, 429, "RATE_LIMITED", "Too many requests. Slow down and retry.");
            return;
        }
        // Content-Length may be absent (chunked transfer) or inaccurate. Bound the actual
        // bytes before Jackson consumes them; read at most limit + 1, never an unbounded body.
        byte[] body = request.getInputStream().readNBytes(Math.toIntExact(props.maxJsonBodyBytes() + 1));
        if (body.length > props.maxJsonBodyBytes()) {
            reject(response, request, 413, "PAYLOAD_TOO_LARGE", "Request body is too large");
            return;
        }
        chain.doFilter(new BufferedRequest(request, body), response);
    }

    /** Drops windows that ended, so the map cannot grow without bound. */
    @Scheduled(fixedDelay = 120_000L)
    public void evictOldWindows() {
        long cutoff = clock.millis() - WINDOW_MILLIS;
        windows.entrySet().removeIf(e -> e.getValue().start < cutoff);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, int status, String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, code, message, request.getRequestURI()));
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Only synchronous JSON body reads are supported");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)));
        }
    }

    private static final class Window {
        private final long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}
