package com.trustbank.loanapp.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.config.AppProperties;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
class RequestGuardFilterTest {
    private RequestGuardFilter guard(int limit, int rate) {
        var security = new AppProperties.Security(false, limit, new AppProperties.RateLimit(rate, rate));
        return new RequestGuardFilter(new AppProperties(null,null,security,null,null,null,null,null,false),
                new ObjectMapper().findAndRegisterModules(), Clock.systemUTC());
    }
    @Test void rejectsOversizedBodyWithoutContentLength() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/applications") {
            @Override public long getContentLengthLong() { return -1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setContent(new byte[17]);
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();
        guard(16,10).doFilter(request,response,(req,res)->called.set(true));
        assertEquals(413,response.getStatus()); assertFalse(called.get());
    }
    @Test void preservesBodyAtExactLimit() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/applications");
        request.setContent("{}".getBytes());
        var response = new MockHttpServletResponse();
        guard(2,10).doFilter(request,response,(req,res)->assertArrayEquals("{}".getBytes(),req.getInputStream().readAllBytes()));
        assertEquals(200,response.getStatus());
    }
    @Test void refusesRequestsAboveRateLimit() throws Exception {
        var filter=guard(16,1);
        filter.doFilter(new MockHttpServletRequest("GET","/api/v1/products"),new MockHttpServletResponse(),(req,res)->{});
        var response=new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET","/api/v1/products"),response,(req,res)->fail("rate limit bypassed"));
        assertEquals(429,response.getStatus()); assertNotNull(response.getHeader("Retry-After"));
    }
}
