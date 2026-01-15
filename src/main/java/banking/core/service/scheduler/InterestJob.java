package banking.core.service.scheduler;

import banking.core.service.InterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestJob {
    private final InterestService interestService;

    @Scheduled(cron = "0 0 2 * * *")
    public void applyDailyInterest() {
        int applied = interestService.applyDailyInterest();
        log.info("Interest job finished. processedAccounts={}", applied);
    }
}
