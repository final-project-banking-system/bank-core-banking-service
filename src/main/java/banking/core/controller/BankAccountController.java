package banking.core.controller;

import banking.core.dto.requests.BalanceOperationRequest;
import banking.core.dto.requests.CreateBankAccountRequest;
import banking.core.dto.requests.UpdateAccountStatusRequest;
import banking.core.dto.responses.BalanceResponse;
import banking.core.dto.responses.BankAccountResponse;
import banking.core.dto.responses.TransferResponse;
import banking.core.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<BankAccountResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CreateBankAccountRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        var result = bankAccountService.createBankAccount(userId, request);
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        var userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.listOfBankAccounts(userId));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> balance(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable("id") UUID accountId) {
        var userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.getBalance(userId, accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountResponse> get(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable("id") UUID accountId) {
        var userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.getBankAccount(userId, accountId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BankAccountResponse> updateStatus(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable("id") UUID accountId,
                                                            @Valid @RequestBody UpdateAccountStatusRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.updateStatus(userId, accountId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> close(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable("id") UUID accountId) {
        var userId = UUID.fromString(jwt.getSubject());
        bankAccountService.closeBankAccount(userId, accountId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransferResponse> deposit(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("id") UUID accountId,
                                                    @Valid @RequestBody BalanceOperationRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        var result = bankAccountService.deposit(userId, accountId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransferResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable("id") UUID accountId,
                                                     @Valid @RequestBody BalanceOperationRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        var result = bankAccountService.withdraw(userId, accountId, request);
        return ResponseEntity.ok(result);
    }
}
