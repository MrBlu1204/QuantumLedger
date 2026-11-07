package com.quantumledger.app.addmoney;

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
class AddMoneyHttpTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // starts high so these users never collide with those made by other test classes
    private static final AtomicLong HTTP_USERS = new AtomicLong(200_000);

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

    private String addMoneyPath(TestUser user) {
        return "/api/v1/accounts/" + walletOf(user).getAccountId() + "/add-money";
    }

    private static String body(long amountMinor, String method, String payerRef) {
        return "{\"amountMinor\":%d,\"method\":\"%s\",\"payerRef\":\"%s\"}"
                .formatted(amountMinor, method, payerRef);
    }

    // ------------- test cases  -------------

    @Test
    void withoutATokenTheRequestIsRejected() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = post(addMoneyPath(asha), body(50_000, "UPI", "asha@okbank"), null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void anApprovedUpiPaymentReturns201CreditsTheWalletAndShowsOnTheStatement() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = post(addMoneyPath(asha), body(50_000, "UPI", "asha@okbank"), asha.token());

        assertEquals(201, response.statusCode(), response.body());
        assertEquals("SUCCESS", field(response.body(), "status"));
        assertEquals("50000", field(response.body(), "amountMinor"));
        assertFalse(field(response.body(), "requestRef").isBlank());
        assertEquals(50_000, balanceOf(walletOf(asha)));

        HttpResponse<String> statement = get(
                "/api/v1/accounts/" + walletOf(asha).getAccountId() + "/statement", asha.token());
        assertEquals(200, statement.statusCode(), statement.body());
        assertEquals("TOP_UP", field(statement.body(), "type"));
        assertEquals("IN", field(statement.body(), "direction"));
        assertEquals("50000", field(statement.body(), "balanceAfterMinor"));
    }

    @Test
    void aDeclinedPaymentReturns422WithAFailedReceiptAndMovesNoMoney() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = post(addMoneyPath(asha), body(50_000, "UPI", "asha@fail"), asha.token());

        assertEquals(422, response.statusCode(), response.body());
        assertEquals("FAILED", field(response.body(), "status"));
        assertEquals("DECLINED_BY_BANK", field(response.body(), "failureReason"));
        assertEquals(0, balanceOf(walletOf(asha)));
    }

    @Test
    void anUnavailableGatewayReturns202WithAPendingReceiptAndMovesNoMoney() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = post(addMoneyPath(asha), body(50_000, "UPI", "asha@timeout"), asha.token());

        assertEquals(202, response.statusCode(), response.body());
        assertEquals("PENDING", field(response.body(), "status"));
        assertEquals(0, balanceOf(walletOf(asha)));
    }

    @Test
    void netBankingIsAcceptedToo() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> response = post(addMoneyPath(asha), body(25_000, "NET_BANKING", "HDFC"), asha.token());

        assertEquals(201, response.statusCode(), response.body());
        assertEquals("SUCCESS", field(response.body(), "status"));
        assertEquals(25_000, balanceOf(walletOf(asha)));
    }

    @Test
    void amountsOutsideTheLimitsReturn422() throws Exception {
        TestUser asha = registerAndLogin("Asha");

        HttpResponse<String> tooSmall = post(addMoneyPath(asha), body(50, "UPI", "asha@okbank"), asha.token());
        HttpResponse<String> tooBig = post(addMoneyPath(asha), body(10_000_001, "UPI", "asha@okbank"), asha.token());

        assertEquals(422, tooSmall.statusCode(), tooSmall.body());
        assertTrue(tooSmall.body().contains("AMOUNT_OUT_OF_RANGE"));
        assertEquals(422, tooBig.statusCode(), tooBig.body());
        assertTrue(tooBig.body().contains("AMOUNT_OUT_OF_RANGE"));
    }

    @Test
    void badBodiesReturn400() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        String path = addMoneyPath(asha);

        HttpResponse<String> zeroAmount = post(path, body(0, "UPI", "asha@okbank"), asha.token());
        HttpResponse<String> noPayerRef = post(path, "{\"amountMinor\":50000,\"method\":\"UPI\"}", asha.token());
        HttpResponse<String> noMethod = post(path, "{\"amountMinor\":50000,\"payerRef\":\"asha@okbank\"}", asha.token());
        HttpResponse<String> unknownMethod = post(path, body(50_000, "BITCOIN", "asha@okbank"), asha.token());
        HttpResponse<String> brokenJson = post(path, "{\"amountMinor\":", asha.token());

        for (HttpResponse<String> response : java.util.List.of(
                zeroAmount, noPayerRef, noMethod, unknownMethod, brokenJson)) {
            assertEquals(400, response.statusCode(), response.body());
            assertTrue(response.body().contains("VALIDATION_FAILED"), response.body());
        }
        assertEquals(0, balanceOf(walletOf(asha)));
    }

    @Test
    void anotherUsersWalletAndAnUnknownWalletBothLookLikeNotFound() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");

        HttpResponse<String> theirs = post(addMoneyPath(ravi), body(50_000, "UPI", "asha@okbank"), asha.token());
        HttpResponse<String> unknown = post("/api/v1/accounts/999999999/add-money",
                body(50_000, "UPI", "asha@okbank"), asha.token());
        HttpResponse<String> notANumber = post("/api/v1/accounts/abc/add-money",
                body(50_000, "UPI", "asha@okbank"), asha.token());

        assertEquals(404, theirs.statusCode(), theirs.body());
        assertTrue(theirs.body().contains("ACCOUNT_NOT_FOUND"));
        assertEquals(404, unknown.statusCode(), unknown.body());
        assertTrue(unknown.body().contains("ACCOUNT_NOT_FOUND"));
        assertEquals(400, notANumber.statusCode(), notANumber.body());
        assertTrue(notANumber.body().contains("VALIDATION_FAILED"));
        assertEquals(0, balanceOf(walletOf(ravi)));
    }

    @Test
    void moneyAddedThroughTheApiCanBeTransferred() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        post(addMoneyPath(asha), body(50_000, "UPI", "asha@okbank"), asha.token());

        HttpResponse<String> transfer = post("/api/v1/transfers",
                "{\"receiverPhone\":\"%s\",\"amountMinor\":20000}".formatted(ravi.phone()), asha.token());

        assertEquals(201, transfer.statusCode(), transfer.body());
        assertEquals(30_000, balanceOf(walletOf(asha)));
        assertEquals(20_000, balanceOf(walletOf(ravi)));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }
}
