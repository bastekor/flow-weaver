package mx.bastekor.flowweaver.config;

import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.model.ThreadContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "flowWeaverExecutor")
    public Executor flowWeaverExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("flow-weaver-");

        // ⭐ IMPORTANTE: TaskDecorator para propagar el contexto
        executor.setTaskDecorator(new FlowWeaverTaskDecorator());

        executor.initialize();

        log.info("✅ FlowWeaver Executor inicializado con TaskDecorator");

        return executor;
    }

    /**
     * TaskDecorator que propaga el contexto de BusinessLog a threads async
     */
    static class FlowWeaverTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            // Capturar el BusinessLog del thread padre ANTES de hacer async
            ThreadContainer threadContainer = FlowWeaverContext.getCurrentThreadContainer();
            String parentThreadName = Thread.currentThread().getName();

            return () -> {
                String asyncThreadName = Thread.currentThread().getName();

                try {
                    // Propagar el contexto al thread hijo
                    FlowWeaverContext.setCurrentThreadContainer(threadContainer);
                    log.debug("🔄 Contexto propagado de [{}] a [{}]", parentThreadName, asyncThreadName);
                    // Ejecutar la tarea async
                    runnable.run();

                } finally {
                    // Limpiar el contexto del thread async después de ejecutar
                    FlowWeaverContext.clearCurrentThreadContainer();
                }
            };
        }
    }
}