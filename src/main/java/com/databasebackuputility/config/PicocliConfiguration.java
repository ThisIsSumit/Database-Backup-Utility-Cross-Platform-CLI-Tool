package com.databasebackuputility.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine;

/**
 * Configuration for Picocli integration with Spring
 */
@Configuration
public class PicocliConfiguration {

    /**
     * Create a factory that can inject Spring beans into Picocli commands
     */
//    @Bean
//    public CommandLine.IFactory picocliFactory(org.springframework.context.ApplicationContext context) {
//        return new CommandLine.IFactory() {
//            @Override
//            public <K> K create(Class<K> clazz) throws Exception {
//                try {
//                    return context.getBean(clazz);
//                } catch (Exception e) {
//                    return CommandLine.defaultFactory().create(clazz);
//                }
//            }
//        };
//    }
}
