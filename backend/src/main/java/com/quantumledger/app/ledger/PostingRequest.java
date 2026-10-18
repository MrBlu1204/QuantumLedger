package com.quantumledger.app.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public record PostingRequest(LedgerTransactionType type,
                             String description,
                             String currency,
                             List<PostingLine> lines) {

    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    public PostingRequest {
        if(type == null){
            throw new InvalidPostingException("type is required");
        }
        if(currency == null || !CURRENCY.matcher(currency).matches()){
            throw new InvalidPostingException("currency must be a 3-letter uppercase code");
        }
        if(description != null && description.length() > 255){
            throw new InvalidPostingException("description is longer than 255 characters");
        }
        if(lines == null || lines.size() < 2){
            throw new InvalidPostingException("a posting needs at least 2 lines");
        }

        lines = List.copyOf(lines);

        long distinctAccounts = lines.stream().map(PostingLine::accountId).distinct().count();
        if(distinctAccounts != lines.size()){
            throw new InvalidPostingException("each account may appear only once in a posting");
        }

        long debits = 0;
        long credits = 0;

        try {
            for(PostingLine line : lines){
                if(line.direction() == Direction.DEBIT){
                    debits = Math.addExact(debits, line.amountMinor());
                }else{
                    credits = Math.addExact(credits, line.amountMinor());
                }
            }
        }catch (ArithmeticException e){
            throw new InvalidPostingException("posting amounts overflow");
        }

        if(debits != credits){
            throw new InvalidPostingException("unbalanced posting: debits=" + debits + " credits=" + credits);
        }
    }

    /** Account ids in ascending order. Locking in this order prevents deadlocks. */
    public List<Long> accountIdsInLockOrder(){
        return lines.stream().map(PostingLine::accountId).sorted().toList();
    }

    public long totalAmountMinor(){
        return lines.stream()
                .filter(l -> l.direction() == Direction.DEBIT)
                .mapToLong(PostingLine::amountMinor)
                .sum();
    }
}
