package com.trustbank.loanapp.idempotency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.IdempotencyRecord;
import com.trustbank.loanapp.repo.IdempotencyRecordRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class IdempotencyServiceTest {
    @Test void lostReplyRetryReturnsCachedResultWithoutRepeatingActionAndRejectsChangedPayload() {
        var records=mock(IdempotencyRecordRepository.class); var cached=new IdempotencyRecord();
        UUID user=UUID.randomUUID(); String key=UUID.randomUUID().toString();
        when(records.claim(any(),eq(user),eq(key),anyString(),anyString(),any(),any())).thenAnswer(inv->{
            if(cached.getRequestHash()!=null)return 0;
            cached.setOperation(inv.getArgument(3)); cached.setRequestHash(inv.getArgument(4)); return 1;
        });
        when(records.storeReply(eq(user),eq(key),anyInt(),anyString())).thenAnswer(inv->{cached.setResponseBody(inv.getArgument(3));return 1;});
        when(records.findByUserIdAndIdemKey(user,key)).thenReturn(Optional.of(cached));
        var props=new AppProperties(null,null,null,null,null,new AppProperties.Idempotency(Duration.ofHours(24)),null,null,false);
        var service=new IdempotencyService(records,new ObjectMapper(),props,Clock.systemUTC());
        var calls=new AtomicInteger();
        var first=service.execute(user,key,"SUBMIT",Map.of("amount",100),String.class,()->{calls.incrementAndGet();return "loan-1";});
        var retry=service.execute(user,key,"SUBMIT",Map.of("amount",100),String.class,()->{calls.incrementAndGet();return "loan-2";});
        assertEquals(first.body(),retry.body()); assertTrue(retry.replayed()); assertEquals(1,calls.get());
        var error=assertThrows(ApiException.class,()->service.execute(user,key,"SUBMIT",Map.of("amount",200),String.class,()->"unexpected"));
        assertEquals("IDEMPOTENCY_KEY_REUSED",error.getCode());
    }
}
