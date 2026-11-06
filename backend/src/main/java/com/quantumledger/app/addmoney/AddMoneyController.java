package com.quantumledger.app.addmoney;


import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/add-money")
public class AddMoneyController {

    private final AddMoneyService addMoneyService;

    public AddMoneyController(AddMoneyService addMoneyService) {
        this.addMoneyService = addMoneyService;
    }

    @PostMapping
    public ResponseEntity<AddMoneyResponse> addMoney(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long accountId,
                                                     @Valid @RequestBody AddMoneyPayload payload){

        Long userId = Long.parseLong(jwt.getSubject());

        AddMoneyService.AddMoneyReceipt receipt = addMoneyService.addMoney(
                new AddMoneyService.AddMoneyCommand(userId, accountId, payload.amountMinor(), payload.method(), payload.payerRef())
        );

        HttpStatus http = switch (receipt.status()){
            case SUCCESS -> HttpStatus.CREATED;
            case PENDING -> HttpStatus.ACCEPTED;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
        };

        return ResponseEntity.status(http).body(AddMoneyResponse.from(receipt));

    }
}
