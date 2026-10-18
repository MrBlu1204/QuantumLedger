package com.quantumledger.app.ledger;

public record PostingLine(Long accountId, Direction direction, long amountMinor) {

    public PostingLine{

        if(accountId == null){
            throw new InvalidPostingException("accountId is required");
        }
        if(direction == null){
            throw new InvalidPostingException("direction is required");
        }
        if(amountMinor <= 0){
            throw new InvalidPostingException("amountMinor must be positive, was " + amountMinor);
        }
    }

    public static PostingLine debit(Long accountId, long amountMinor) {
        return new PostingLine(accountId, Direction.DEBIT, amountMinor);
    }

    public static PostingLine credit(Long accountId, long amountMinor) {
        return new PostingLine(accountId, Direction.CREDIT, amountMinor);
    }
}
