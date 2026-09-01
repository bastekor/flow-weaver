package mx.bastekor.flowweaver.config;

import mx.bastekor.flowweaver.handler.FlowWeaverResultHandler;
import mx.bastekor.flowweaver.handler.LoggingFlowWeaverResultHandler;
import mx.bastekor.flowweaver.util.FrameFormatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "mx.bastekor.flowweaver")
public class FlowWeaverConfig {

    @Bean
    @ConditionalOnMissingBean(FlowWeaverResultHandler.class)
    FlowWeaverResultHandler flowWeaverResultHandler(FrameConfig frameConfig, FrameFormatter frameFormatter) {
        return new LoggingFlowWeaverResultHandler(frameConfig, frameFormatter);
    }
}