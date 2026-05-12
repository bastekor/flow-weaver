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
            ThreadContainer parentThreadContainer = FlowWeaverContext.getCurrentThreadContainer();
            String parentThreadName = Thread.currentThread().getName();

            return () -> {
                String asyncThreadName = Thread.currentThread().getName();

                // 🔹 Detectar si ya existe uno en el hijo
                boolean contextoPrevio = FlowWeaverContext.peekThreadContainerExists();

                try {
                    if (!contextoPrevio) {
                        // Propagar sólo si el hijo NO tenía uno propio
                        FlowWeaverContext.setCurrentThreadContainer(parentThreadContainer);
                        log.debug("🔄 Contexto propagado de [{}] a [{}]", parentThreadName, asyncThreadName);
                    }

                    runnable.run();

                } finally {
                    // 🔸 Solo limpiar si el hilo async NO heredó el contexto del padre
                    if (!contextoPrevio) {
                        FlowWeaverContext.clearCurrentThreadContainer();
                    }
                }
            };
        }
    }
}