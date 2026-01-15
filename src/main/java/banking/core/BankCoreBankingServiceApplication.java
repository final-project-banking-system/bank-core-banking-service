package banking.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BankCoreBankingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCoreBankingServiceApplication.class, args);
    }

}
