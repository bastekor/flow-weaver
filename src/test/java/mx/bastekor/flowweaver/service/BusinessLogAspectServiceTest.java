package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.config.InfoAppConfig;
import mx.bastekor.flowweaver.factory.BusinessLogEventFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessLogAspectServiceTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private InfoAppConfig infoAppConfig;

    @Mock
    private BusinessLogConfig businessLogConfig;

    @InjectMocks
    private BusinessLogAspectService businessLogAspectService;

    @Test
    void test_enqueue() {
        var event = BusinessLogEventFactory.create();
        businessLogAspectService.enqueue(event);
        verify(executorService, times(1)).submit((Callable<Object>) any());
    }
}