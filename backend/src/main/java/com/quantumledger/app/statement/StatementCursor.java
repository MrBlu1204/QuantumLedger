package com.quantumledger.app.statement;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class StatementCursor {

    private StatementCursor() { }

    static String encode(long entryId){
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(entryId).getBytes(StandardCharsets.UTF_8));
    }

    static long decode(String cursor){
        try{
            String text = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            long entryId = Long.parseLong(text);
            if(entryId <= 0){
                throw new NumberFormatException();
            }
            return entryId;
        }catch (IllegalArgumentException e){ // bad Base64 or NumberFormatException
            throw new InvalidStatementRequestException("INVALID_CURSOR","cursor is not valid");
        }
    }
}
