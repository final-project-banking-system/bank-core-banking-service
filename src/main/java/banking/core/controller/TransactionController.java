package banking.core.controller;

import banking.core.dto.requests.TransactionFilter;
import banking.core.dto.responses.TransactionResponse;
import banking.core.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> history(@AuthenticationPrincipal Jwt jwt,
                                                             @ModelAttribute TransactionFilter filter,
                                                             @PageableDefault(size = 20, sort = "createdAt",
                                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        var userId = UUID.fromString(jwt.getSubject());
        var result = transactionService.getHistoryOfTransactions(filter, userId, pageable);
        return ResponseEntity.ok(result);
    }
}
