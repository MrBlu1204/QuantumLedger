package com.quantumledger.app.statement;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping("/{accountId}/statement")
    public StatementPage statement(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable long accountId,
                                   @RequestParam(required = false) Integer limit,
                                   @RequestParam(required = false) String cursor){

        long userId =  Long.parseLong(jwt.getSubject());
        return statementService.getStatement(userId, accountId, limit, cursor);
    }
}
