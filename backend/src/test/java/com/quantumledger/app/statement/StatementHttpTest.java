package com.quantumledger.app.statement;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatementHttpTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // starts high so these users never collide with those made by other test classes
    private static final AtomicLong HTTP_USERS = new AtomicLong(100_000);

    private record TestUser(long userId, String phone, String token) { }

    // --------- helper methods ---------

    private HttpResponse<String> send(HttpRequest.Builder request, String bearer) throws Exception {
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return client.send(request.timeout(Duration.ofSeconds(30)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json, String bearer) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)), bearer);
    }

    private HttpResponse<String> get(String path, String bearer) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET(), bearer);
    }

    /** Pulls a simple value (the first match) out of a JSON string, without a JSON library. */
    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"?([^\",}]+)").matcher(json);
        assertTrue(m.find(), "field '" + name + "' not found in: " + json);
        return m.group(1);
    }

    private TestUser registerAndLogin(String firstName) throws Exception {
        long n = HTTP_USERS.getAndIncrement();
        String email = firstName.toLowerCase() + n + "@example.com";
        String phone = String.format("8%09d", n);

        HttpResponse<String> signup = post("/api/v1/auth/signup", """
                {"firstName":"%s","lastName":"Test","email":"%s","phone":"%s","password":"StrongPass123"}
                """.formatted(firstName, email, phone), null);
        assertEquals(201, signup.statusCode(), signup.body());

        HttpResponse<String> login = post("/api/v1/auth/login", """
                {"email":"%s","password":"StrongPass123"}
                """.formatted(email), null);
        assertEquals(200, login.statusCode(), login.body());

        return new TestUser(Long.parseLong(field(signup.body(), "userId")), phone,
                field(login.body(), "accessToken"));
    }

    private Account walletOf(TestUser user) {
        return accountRepository.findByUserIdAndCurrency(user.userId(), "INR").orElseThrow();
    }

    private void transfer(TestUser from, TestUser to, long amountMinor) throws Exception {
        HttpResponse<String> response = post("/api/v1/transfers",
                "{\"receiverPhone\":\"%s\",\"amountMinor\":%d}".formatted(to.phone(), amountMinor),
                from.token());
        assertEquals(201, response.statusCode(), response.body());
    }

    private String statementPath(TestUser user, String query) {
        return "/api/v1/accounts/" + walletOf(user).getAccountId() + "/statement" + query;
    }

    // ------------- test cases  -------------

    @Test
    void statementWithoutATokenIsRejected() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = get(statementPath(asha, ""), null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void showsEachSideOfATransferWithDirectionCounterpartyAndRunningBalance() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);
        transfer(asha, ravi, 25_000);

        HttpResponse<String> ashaView = get(statementPath(asha, ""), asha.token());
        HttpResponse<String> raviView = get(statementPath(ravi, ""), ravi.token());

        assertEquals(200, ashaView.statusCode(), ashaView.body());
        assertEquals("OUT", field(ashaView.body(), "direction"));
        assertEquals("25000", field(ashaView.body(), "amountMinor"));
        assertEquals("75000", field(ashaView.body(), "balanceAfterMinor"));
        assertEquals("Ravi", field(ashaView.body(), "counterpartyFirstName"));
        assertEquals("false", field(ashaView.body(), "hasMore"));
        assertFalse(ashaView.body().contains("\"nextCursor\":\""));

        assertEquals(200, raviView.statusCode(), raviView.body());
        assertEquals("IN", field(raviView.body(), "direction"));
        assertEquals("25000", field(raviView.body(), "balanceAfterMinor"));
        assertEquals("Asha", field(raviView.body(), "counterpartyFirstName"));
    }

    @Test
    void walksThroughPagesUsingTheCursor() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);
        for (long amount = 100; amount <= 500; amount += 100) {
            transfer(asha, ravi, amount);
        }

        // newest first: 500, 400 | 300, 200 | 100, funding
        HttpResponse<String> page1 = get(statementPath(asha, "?limit=2"), asha.token());
        assertEquals(200, page1.statusCode(), page1.body());
        assertEquals("500", field(page1.body(), "amountMinor"));
        assertEquals("true", field(page1.body(), "hasMore"));

        HttpResponse<String> page2 = get(
                statementPath(asha, "?limit=2&cursor=" + field(page1.body(), "nextCursor")), asha.token());
        assertEquals("300", field(page2.body(), "amountMinor"));
        assertEquals("true", field(page2.body(), "hasMore"));

        HttpResponse<String> page3 = get(
                statementPath(asha, "?limit=2&cursor=" + field(page2.body(), "nextCursor")), asha.token());
        assertEquals("100", field(page3.body(), "amountMinor"));
        assertEquals("false", field(page3.body(), "hasMore"));
        assertTrue(page3.body().contains("100000"), page3.body());   // the funding line is last
    }

    @Test
    void anotherUsersWalletAndAnUnknownWalletBothLookLikeNotFound() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");

        HttpResponse<String> theirs = get(
                "/api/v1/accounts/" + walletOf(ravi).getAccountId() + "/statement", asha.token());
        HttpResponse<String> unknown = get("/api/v1/accounts/999999999/statement", asha.token());

        assertEquals(404, theirs.statusCode(), theirs.body());
        assertTrue(theirs.body().contains("ACCOUNT_NOT_FOUND"));
        assertEquals(404, unknown.statusCode(), unknown.body());
        assertTrue(unknown.body().contains("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void badLimitsAndCursorsReturn400() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> zero = get(statementPath(asha, "?limit=0"), asha.token());
        HttpResponse<String> tooBig = get(statementPath(asha, "?limit=201"), asha.token());
        HttpResponse<String> notANumber = get(statementPath(asha, "?limit=abc"), asha.token());
        HttpResponse<String> badCursor = get(statementPath(asha, "?cursor=***"), asha.token());

        assertEquals(400, zero.statusCode(), zero.body());
        assertTrue(zero.body().contains("VALIDATION_FAILED"));
        assertEquals(400, tooBig.statusCode(), tooBig.body());
        assertTrue(tooBig.body().contains("VALIDATION_FAILED"));
        assertEquals(400, notANumber.statusCode(), notANumber.body());
        assertTrue(notANumber.body().contains("VALIDATION_FAILED"));
        assertEquals(400, badCursor.statusCode(), badCursor.body());
        assertTrue(badCursor.body().contains("INVALID_CURSOR"));
    }
}
