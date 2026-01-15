package banking.core.controller;

import banking.core.dto.requests.TransferRequest;
import banking.core.dto.responses.TransferResponse;
import banking.core.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody TransferRequest request) {
        var userId = UUID.fromString(jwt.getSubject());
        var result = transferService.transfer(userId, request);
        return ResponseEntity.ok(result);
    }
}
