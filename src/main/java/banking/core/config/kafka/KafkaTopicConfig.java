package banking.core.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaTopicConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    private List<TopicConfig> topics;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        configs.put("offsets.topic.replication.factor", "1");
        configs.put("transaction.state.log.replication.factor", "1");
        configs.put("transaction.state.log.min.isr", "1");
        return new KafkaAdmin(configs);
    }

    @Bean
    public List<NewTopic> createTopic() {
        return topics.stream().map(topic -> TopicBuilder.name(topic.getName())
                        .replicas(topic.getReplicationFactor())
                        .partitions(topic.getPartitions())
                        .build())
                .toList();
    }

    @Getter
    @Setter
    public static class TopicConfig {
        private String name;
        private int partitions;
        private short replicationFactor;
    }
}
