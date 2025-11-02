package com.platform.orchestrator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${orchestrator.kafka.topics.job-tasks}")
    private String jobTasksTopic;

    @Value("${orchestrator.kafka.topics.job-results}")
    private String jobResultsTopic;

    @Bean
    public NewTopic jobTasksTopic() {
        return TopicBuilder.name(jobTasksTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobResultsTopic() {
        return TopicBuilder.name(jobResultsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
