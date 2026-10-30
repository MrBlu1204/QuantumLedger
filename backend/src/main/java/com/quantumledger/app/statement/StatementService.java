package com.quantumledger.app.statement;


import com.quantumledger.app.account.AccountNotFoundException;
import com.quantumledger.app.statement.StatementRepository.StatementRow;
import com.quantumledger.app.account.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatementService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    private final StatementRepository statementRepository;

    public StatementService(StatementRepository statementRepository) {
        this.statementRepository = statementRepository;

    }

    @Transactional(readOnly = true)
    public StatementPage getStatement(long userId, long accountId, Integer limit, String cursor){
        int pageSize = limit == null ? DEFAULT_PAGE_SIZE : limit;

        if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
            throw new InvalidStatementRequestException("VALIDATION_FAILED",
                    "limit must be between 1 and " + MAX_PAGE_SIZE);
        }

        long before = cursor == null ?  Long.MAX_VALUE : StatementCursor.decode(cursor);

        if(!statementRepository.ownsWallet(userId, accountId)){
            throw new AccountNotFoundException("account not found");
        }

        //one extra row tells us whether next page exist, with no COUNT(*)

        List<StatementRow> rows = statementRepository.findPage(userId, accountId, before, pageSize + 1);

        boolean hasMore = rows.size() > pageSize;

        List<StatementRow> pageRows = hasMore ? rows.subList(0, pageSize) : rows;

        List<StatementEntry> items = pageRows.stream().map(StatementRow::entry).toList();
        String nextCursor = hasMore
                ? StatementCursor.encode(pageRows.get(pageRows.size() -1).entryId()) : null;

        return new StatementPage(items, nextCursor, hasMore);

    }
}
