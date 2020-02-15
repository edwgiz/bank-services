package com.github.edwgiz.sample.bank.account.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.edwgiz.sample.bank.account.jooq.tables.interfaces.IPayment;
import com.github.edwgiz.sample.bank.account.jooq.tables.pojos.Payment;
import com.github.edwgiz.sample.bank.account.model.NewPayment;
import org.glassfish.jersey.server.ResourceConfig;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.edwgiz.sample.bank.account.jooq.tables.Account.ACCOUNT;
import static com.github.edwgiz.sample.bank.account.jooq.tables.Payment.PAYMENT;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class PaymentEndpointTest extends AbstractEndpointTest {

    private static final long ACCOUNT0_ID = 0L;
    private static final long ACCOUNT2_ID = 2L;

    // data to share between different tests
    private static final AtomicReference<Payment> PAYMENT_1 = new AtomicReference<>();
    private static final AtomicReference<Payment> PAYMENT_2 = new AtomicReference<>();


    @Inject
    private DSLContext dslCtx;


    protected ResourceConfig configure() {
        return super.configure().register(PaymentEndpoint.class);
    }


    /**
     * Tests successful '/payment' PUT.
     */
    @Test
    public void o1testCreate() throws IOException, InterruptedException {
        testCreateValidations();

        final Payment payment1 = testCreate(newPayment(TEN, ACCOUNT0_ID, ACCOUNT2_ID, "Test Payment 1"));
        Assertions.assertNotNull(payment1);
        PAYMENT_1.set(payment1);
        final long distinguishTime = 10L;
        Thread.sleep(distinguishTime); // distinguish by time to simplify a following 'list' test
        final Payment payment2 = testCreate(newPayment(ONE, ACCOUNT2_ID, ACCOUNT0_ID, null));
        Assertions.assertNotNull(payment2);
        PAYMENT_2.set(payment2);

        testCreateWithLockedAccount();
    }

    private void testCreateWithLockedAccount() throws JsonProcessingException, InterruptedException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final CountDownLatch lockCompleted = new CountDownLatch(1);
            final CountDownLatch testCompleted = new CountDownLatch(1);
            executor.submit(() -> dslCtx.transaction((cnf) -> {
                final SelectQuery<Record> select = cnf.dsl().selectQuery(ACCOUNT);
                select.addConditions(ACCOUNT.ACCOUNT_ID.eq(ACCOUNT0_ID));
                select.setForUpdate(true);
                select.execute();
                lockCompleted.countDown();
                testCompleted.await();
            }));
            lockCompleted.await();
            testCreateWhenAccount0IsLocked();
            testCompleted.countDown();
        } finally {
            executor.shutdown();
        }
    }

    private void testCreateWhenAccount0IsLocked() throws JsonProcessingException {
        testSimpleResponse(this::invokeCreate, newPaymentAsJson(ONE, ACCOUNT0_ID, ACCOUNT2_ID, null),
                CONFLICT, TEXT_PLAIN_TYPE);
        testSimpleResponse(this::invokeCreate, newPaymentAsJson(ONE, ACCOUNT2_ID, ACCOUNT0_ID, null),
                CONFLICT, TEXT_PLAIN_TYPE);
    }

    private void testCreateValidations() throws JsonProcessingException {
        testCreateValidation(" wrong json ");

        testCreateValidation(newPaymentAsJson(// empty entity
                null, null, null, null));
        testCreateValidation(newPaymentAsJson(// null amount
                null, ACCOUNT0_ID, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// null withdrawal account
                ONE, null, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// null deposit account
                ONE, ACCOUNT0_ID, null, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// zero amount
                BigDecimal.ZERO, ACCOUNT0_ID, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// the withdrawal account is equal to the deposit account
                ONE, ACCOUNT2_ID, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// the withdrawal account not exists
                ONE, Long.MAX_VALUE, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// the deposit account not exists
                ONE, ACCOUNT0_ID, Long.MAX_VALUE, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// insufficient funds
                new BigDecimal(Long.MAX_VALUE), ACCOUNT0_ID, ACCOUNT2_ID, "Payment Comment 1"));
        testCreateValidation(newPaymentAsJson(// too long comment
                ONE, ACCOUNT0_ID, ACCOUNT2_ID, repeat('A', PAYMENT.COMMENT.getDataType().length() + 1)));
    }


    private String newPaymentAsJson(final BigDecimal amount, final Long withdrawalAccountId,
            final Long depositAccountId, final String comment) throws JsonProcessingException {
        return getJson().writeValueAsString(
                newPayment(amount, withdrawalAccountId, depositAccountId, comment));
    }

    private NewPayment newPayment(final BigDecimal amount, final Long withdrawalAccountId, final Long depositAccountId,
            final String comment) {
        final NewPayment result = new NewPayment();
        result.setAmount(amount);
        result.setWithdrawalAccountId(withdrawalAccountId);
        result.setDepositAccountId(depositAccountId);
        result.setComment(comment);
        return result;
    }

    private void testCreateValidation(final String requestBody) {
        testSimpleResponse(this::invokeCreate, requestBody, BAD_REQUEST, TEXT_PLAIN_TYPE);
    }

    private Payment testCreate(final NewPayment value) throws JsonProcessingException {
        final LocalDateTime intervalFrom = now(UTC).truncatedTo(MILLIS).minus(1, MILLIS);
        final Response resp = invokeCreate(getJson().writeValueAsString(value));
        final LocalDateTime intervalTo = now(UTC).plus(1, MILLIS);

        assertEquals(OK, resp.getStatusInfo());
        assertEquals(TEXT_PLAIN_TYPE, resp.getMediaType());
        assertTrue(resp.hasEntity());
        final long newPaymentId = new Scanner((InputStream) resp.getEntity(), US_ASCII.name()).nextLong();

        final Payment newPayment = dslCtx.selectFrom(PAYMENT).where(PAYMENT.PAYMENT_ID.eq(newPaymentId))
                .fetchAnyInto(Payment.class);

        equals(value, between(intervalFrom, intervalTo), newPayment);

        return newPayment;
    }

    private void equals(final IPayment expected, final Predicate<LocalDateTime> expectedProcessed,
            final Payment actual) {
        assertEquals(0, expected.getAmount().compareTo(actual.getAmount()));
        assertTrue(expectedProcessed.test(actual.getProcessed()));
        assertEquals(expected.getWithdrawalAccountId(), actual.getWithdrawalAccountId());
        assertEquals(expected.getDepositAccountId(), actual.getDepositAccountId());
        assertEquals(expected.getComment(), actual.getComment());
    }

    private Response invokeCreate(final String value) {
        return target().path("/payment").request().put(Entity.json(value));
    }


    /**
     * Tests '/payment' PUT when a payment record can't be saved.
     */
    @Test
    public void o2testInsertPaymentFail() {
        final PaymentEndpoint endpoint = new PaymentEndpoint(null);
        Assertions.assertThrows(IllegalStateException.class,
                () -> endpoint.insertPaymentCheckUpdatedRows(0),
                "Can't create payment record");
    }


    /**
     * Tests '/payment' PUT when a balance of one of given accounts can't be changed.
     */
    @Test
    public void o3testUpdateAccountFail() {
        final PaymentEndpoint endpoint = new PaymentEndpoint(null);
        Assertions.assertThrows(IllegalStateException.class,
                () -> endpoint.updateAccountCheckUpdatedRows(0, "some"),
                "Can't update balance of some account");
    }


    /**
     * Tests '/payment' GET.
     */
    @Test
    public void o4testList() throws IOException {
        final String intervalFrom = "0000-01-01T00:00:00.000";
        final String intervalTo = "9999-12-31T23:59:59.999";

        testListValidation(null, null, null, NOT_FOUND);
        testListValidation(ACCOUNT0_ID, intervalFrom, null, NOT_FOUND);
        testListValidation(ACCOUNT0_ID, null, intervalTo, NOT_FOUND);
        testListValidation(null, intervalFrom, intervalTo, BAD_REQUEST);
        testListValidation("X", intervalFrom, intervalTo, NOT_FOUND);
        testListValidation(ACCOUNT0_ID, "X", intervalTo, NOT_FOUND);
        testListValidation(ACCOUNT0_ID, intervalFrom, "X", NOT_FOUND);
        testListValidation(ACCOUNT0_ID, intervalTo, intervalFrom, BAD_REQUEST);

        testList(Long.MAX_VALUE, intervalFrom, intervalTo, emptyMap());
        testList(ACCOUNT2_ID, intervalFrom, format(PAYMENT_1.get().getProcessed()),
                singletonMap(PAYMENT_1.get().getPaymentId(), PAYMENT_1.get()));

        final Map<Long, Payment> payments = Stream.of(PAYMENT_1.get(), PAYMENT_2.get())
                .collect(toMap(Payment::getPaymentId, o -> o));
        testList(ACCOUNT0_ID, intervalFrom, intervalTo, payments);
        testList(ACCOUNT2_ID, intervalFrom, intervalTo, payments);
    }

    /* default */void testList(final Long accountId, final String intervalFrom, final String intervalTo,
            final Map<Long, Payment> payments) throws IOException {
        final Response resp = invokeList(accountId, intervalFrom, intervalTo);
        assertEquals(OK, resp.getStatusInfo());
        assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType());
        final ObjectReader objectReader = getJson().readerFor(Payment[].class);
        final Payment[] result = objectReader.readValue((InputStream) resp.getEntity());

        assertEquals(payments.size(), result.length);
        for (final Payment actual : result) {
            final Payment expected = payments.get(actual.getPaymentId());
            equals(expected, wideTo(expected.getProcessed(), 2, MILLIS), actual);
        }
    }

    /* default */void testListValidation(final Object accountId, final String intervalFrom, final String intervalTo,
            final Response.Status expectedStatus) {
        final Response resp = invokeList(accountId, intervalFrom, intervalTo);
        assertEquals(expectedStatus, resp.getStatusInfo());
        assertEquals(TEXT_PLAIN_TYPE, resp.getMediaType());
        assertTrue(resp.hasEntity());
    }

    private Response invokeList(final Object accountId, final String intervalFrom, final String intervalTo) {
        return target().path("/payment")
                .queryParam("accountId", accountId)
                .queryParam("from", intervalFrom)
                .queryParam("to", intervalTo)
                .request().get();
    }

    @AfterClass
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static void afterClass() {
        PAYMENT_1.set(null);
        PAYMENT_2.set(null);
    }
}
