package com.quantumledger.app.transfer;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferFacade transferFacade;

    public TransferController(TransferFacade transferFacade) {
        this.transferFacade = transferFacade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody TransferRequest request) {
        Long senderId = Long.parseLong(jwt.getSubject());
        String countryCode = request.receiverCountryCode() == null ? "+91"  : request.receiverCountryCode();

        TransferService.TransferReceipt receipt =
                transferFacade.transfer(new TransferService.TransferCommand(senderId, countryCode,
                        request.receiverPhone(), request.amountMinor()));

        return TransferResponse.from(receipt);
    }
}
