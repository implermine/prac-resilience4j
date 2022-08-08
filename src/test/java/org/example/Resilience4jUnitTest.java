package org.example;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.mockito.Mockito.*;

class Resilience4jUnitTest {

    interface RemoteService{
        int process(int i);
    }

    private RemoteService service;

    @BeforeEach
    public void setUp(){
        service = mock(RemoteService.class);
    }


    /**
     * Ref: Baeldung
     * https://www.baeldung.com/resilience4j
     */
    @Test
    void shouldWorkAsExpected_WhenCircuitBreakerIsUsed() {

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        //Deprecated
        //CircuitBreakerConfig.custom().failureRateThreshold(20).ringBufferSizeInClosedState(5);

        /**
         * set the rate threshold to 20% and a minimum number of 5 call attempts.
         */
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Percentage of failures to start short-circuit
                .failureRateThreshold(20)
                // Min number of call attempts
                .slidingWindow(5, 5, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("my");
        
        //Annotation이 아닌 decorating
        Function<Integer,Integer> decorated = CircuitBreaker.decorateFunction(circuitBreaker,service::process);

        //Mock RemoteService to throw exception
        when(service.process(anyInt())).thenThrow(new RuntimeException());

        for(int i =0; i<10;i++){
            try{
                decorated.apply(i); // Mocking된 service를 5번 호출 (5번 다 exception을 발생시킬 예정)
            }catch (Exception ignore){
                System.out.println(ignore.getClass());
                /**
                 * class java.lang.RuntimeException
                 * class java.lang.RuntimeException
                 * class java.lang.RuntimeException
                 * class java.lang.RuntimeException
                 * class java.lang.RuntimeException
                 * class io.github.resilience4j.circuitbreaker.CallNotPermittedException
                 * class io.github.resilience4j.circuitbreaker.CallNotPermittedException
                 * class io.github.resilience4j.circuitbreaker.CallNotPermittedException
                 * class io.github.resilience4j.circuitbreaker.CallNotPermittedException
                 * class io.github.resilience4j.circuitbreaker.CallNotPermittedException
                 */
            }
        }

        verify(service,times(5)).process(any(Integer.class));
    }


}