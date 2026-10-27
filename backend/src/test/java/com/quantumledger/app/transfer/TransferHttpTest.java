package com.quantumledger.app.transfer;


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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferHttpTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    private static final AtomicLong HTTP_USERS = new AtomicLong(1);

    private record TestUser(long userId, String phone, String token) { }

    private record Call(String token, String json){ }

    // --------- helper methods ---------

    private HttpResponse<String> post(String path, String json, String brearer) throws Exception{
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if(brearer != null){
            request.header("Authorization", "Bearer " + brearer);
        }

        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Pulls a simple value out of a JSON string, without needing a JSON library. */
    private static String field(String json, String name){
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

    private HttpResponse<String> transfer(TestUser from, String receiverPhone, long amountMinor) throws Exception {
        return post("/api/v1/transfers", body(receiverPhone, amountMinor), from.token());
    }

    private static String body(String receiverPhone, long amountMinor) {
        return "{\"receiverPhone\":\"%s\",\"amountMinor\":%d}".formatted(receiverPhone, amountMinor);
    }

    private List<Integer> postConcurrently(List<Call> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (Call call : calls) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return post("/api/v1/transfers", call.json(), call.token()).statusCode();
                }));
            }
            startGate.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------- test cases  -------------

    @Test
    void transferWithoutATokenIsRejected() throws Exception {
        HttpResponse<String> response = post("/api/v1/transfers", body("8999999999", 500), null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);

        String token = asha.token();
        int middle = token.length() / 2;
        char replacement = token.charAt(middle) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, middle) + replacement + token.substring(middle + 1);

        HttpResponse<String> response = post("/api/v1/transfers", body(ravi.phone(), 500), tampered);

        assertEquals(401, response.statusCode());
        assertEquals(100_000, balanceOf(walletOf(asha)));
    }

    @Test
    void aSuccessfulTransferReturns201AndMovesTheMoney() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);

        HttpResponse<String> response = transfer(asha, ravi.phone(), 25_000);

        assertEquals(201, response.statusCode(), response.body());
        assertEquals("COMPLETED", field(response.body(), "status"));
        assertEquals("25000", field(response.body(), "amountMinor"));
        assertEquals("Ravi", field(response.body(), "firstName"));
        assertEquals(75_000, balanceOf(walletOf(asha)));
        assertEquals(25_000, balanceOf(walletOf(ravi)));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void theSenderComesFromTheTokenNeverFromTheBody() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);
        fund(walletOf(ravi), 50_000);

        // Asha's token, but the body falsely names Ravi as the sender.
        String sneaky = "{\"senderId\":%d,\"receiverPhone\":\"%s\",\"amountMinor\":25000}"
                .formatted(ravi.userId(), ravi.phone());
        HttpResponse<String> response = post("/api/v1/transfers", sneaky, asha.token());

        // The request is either accepted with the fake field ignored, or rejected outright.
        // Either way, Ravi must never be the one who pays.
        assertTrue(response.statusCode() == 201 || response.statusCode() == 400, response.body());
        if (response.statusCode() == 201) {
            assertEquals(75_000, balanceOf(walletOf(asha)));    // Asha paid
            assertEquals(75_000, balanceOf(walletOf(ravi)));    // Ravi RECEIVED 25,000
        } else {
            assertEquals(100_000, balanceOf(walletOf(asha)));
            assertEquals(50_000, balanceOf(walletOf(ravi)));
        }
    }


    @Test
    void malformedRequestsReturn400() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);

        HttpResponse<String> zero = transfer(asha, ravi.phone(), 0);
        HttpResponse<String> badPhone = transfer(asha, "12345", 500);
        HttpResponse<String> missingAmount = post("/api/v1/transfers",
                "{\"receiverPhone\":\"" + ravi.phone() + "\"}", asha.token());

        assertEquals(400, zero.statusCode(), zero.body());
        assertEquals(400, badPhone.statusCode(), badPhone.body());
        assertEquals(400, missingAmount.statusCode(), missingAmount.body());
        assertTrue(zero.body().contains("VALIDATION_FAILED"));
        assertEquals(100_000, balanceOf(walletOf(asha)));
    }


    @Test
    void businessRuleViolationsReturn422() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 1_000);

        HttpResponse<String> tooSmall = transfer(asha, ravi.phone(), 50);
        HttpResponse<String> toSelf = transfer(asha, asha.phone(), 500);
        HttpResponse<String> tooMuch = transfer(asha, ravi.phone(), 5_000);

        assertEquals(422, tooSmall.statusCode(), tooSmall.body());
        assertTrue(tooSmall.body().contains("AMOUNT_OUT_OF_RANGE"));
        assertEquals(422, toSelf.statusCode(), toSelf.body());
        assertTrue(toSelf.body().contains("CANNOT_TRANSFER_TO_SELF"));
        assertEquals(422, tooMuch.statusCode(), tooMuch.body());
        assertTrue(tooMuch.body().contains("INSUFFICIENT_FUNDS"));
        assertEquals(1_000, balanceOf(walletOf(asha)));
        assertEquals(0, balanceOf(walletOf(ravi)));
    }


    @Test
    void anUnregisteredReceiverReturns404() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        fund(walletOf(asha), 1_000);

        HttpResponse<String> response = transfer(asha, "7000000000", 500);

        assertEquals(404, response.statusCode(), response.body());
        assertTrue(response.body().contains("RECEIVER_NOT_FOUND"));
    }

    @Test
    void aBlockedReceiverReturns422() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 1_000);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE user_id = ?", ravi.userId());

        HttpResponse<String> response = transfer(asha, ravi.phone(), 500);

        assertEquals(422, response.statusCode(), response.body());
        assertTrue(response.body().contains("RECEIVER_UNAVAILABLE"));
        assertEquals(1_000, balanceOf(walletOf(asha)));
    }


    @Test
    void aBlockedSenderIsRefusedEvenWithAStillValidToken() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 1_000);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE user_id = ?", asha.userId());

        HttpResponse<String> response = transfer(asha, ravi.phone(), 500);

        assertEquals(403, response.statusCode(), response.body());
        assertTrue(response.body().contains("ACCOUNT_NOT_ACTIVE"));
        assertEquals(1_000, balanceOf(walletOf(asha)));
    }


    @Test
    void thirtyParallelRequestsOnOneWalletNeverOverspend() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 10_000);

        List<Call> calls = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            calls.add(new Call(asha.token(), body(ravi.phone(), 500)));
        }

        List<Integer> statuses = postConcurrently(calls);

        assertEquals(20, Collections.frequency(statuses, 201), "statuses: " + statuses);
        assertEquals(10, Collections.frequency(statuses, 422), "statuses: " + statuses);
        assertEquals(0, balanceOf(walletOf(asha)));
        assertEquals(10_000, balanceOf(walletOf(ravi)));
        assertEquals(20L, jdbc.queryForObject("SELECT COUNT(*) FROM transfers", Long.class));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void oppositeParallelTransfersAllSucceedWithoutDeadlocks() throws Exception {
        TestUser asha = registerAndLogin("Asha");
        TestUser ravi = registerAndLogin("Ravi");
        fund(walletOf(asha), 100_000);
        fund(walletOf(ravi), 100_000);

        List<Call> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(new Call(asha.token(), body(ravi.phone(), 100)));
            calls.add(new Call(ravi.token(), body(asha.phone(), 100)));
        }

        List<Integer> statuses = postConcurrently(calls);

        assertEquals(40, Collections.frequency(statuses, 201), "statuses: " + statuses);
        assertEquals(100_000, balanceOf(walletOf(asha)));
        assertEquals(100_000, balanceOf(walletOf(ravi)));
        assertEquals(40L, jdbc.queryForObject("SELECT COUNT(*) FROM transfers", Long.class));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }












}
