package com.quantumledger.app.statement;

import java.util.List;

public record StatementPage(
        List<StatementEntry> items,
        String nextCursor,
        boolean hasMore
) {
}
