package banking.core.it;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class IntegrationTestBase {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("core_db")
                    .withUsername("postgres")
                    .withPassword("postgres");
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName
            .parse("apache/kafka:3.9.1"));

    @BeforeAll
    static void startContainer() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (!KAFKA.isRunning()) {
            KAFKA.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.task.scheduling.enabled", () -> "false");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("banking.kafka.topics.transfers", () -> "banking.transfers");
        registry.add("banking.kafka.topics.accounts", () -> "banking.accounts");
        registry.add("banking.kafka.topics.transactions", () -> "banking.transactions");
        registry.add("banking.kafka.topics.systemErrors", () -> "system.errors");

        registry.add("spring.kafka.topics[0].name", () -> "banking.transfers");
        registry.add("spring.kafka.topics[0].partitions", () -> "1");
        registry.add("spring.kafka.topics[0].replication-factor", () -> "1");

        registry.add("spring.kafka.topics[1].name", () -> "banking.accounts");
        registry.add("spring.kafka.topics[1].partitions", () -> "1");
        registry.add("spring.kafka.topics[1].replication-factor", () -> "1");

        registry.add("spring.kafka.topics[2].name", () -> "banking.transactions");
        registry.add("spring.kafka.topics[2].partitions", () -> "1");
        registry.add("spring.kafka.topics[2].replication-factor", () -> "1");

        registry.add("spring.kafka.topics[3].name", () -> "system.errors");
        registry.add("spring.kafka.topics[3].partitions", () -> "1");
        registry.add("spring.kafka.topics[3].replication-factor", () -> "1");
    }
}
