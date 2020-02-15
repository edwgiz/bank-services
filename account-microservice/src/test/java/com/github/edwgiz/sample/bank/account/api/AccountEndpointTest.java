package com.github.edwgiz.sample.bank.account.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.edwgiz.sample.bank.account.jooq.tables.pojos.Account;
import com.github.edwgiz.sample.bank.account.model.NewAccount;
import com.github.edwgiz.sample.bank.account.model.UpdateAccount;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.edwgiz.sample.bank.account.jooq.tables.Account.ACCOUNT;
import static java.math.BigDecimal.ZERO;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class AccountEndpointTest extends AbstractEndpointTest {

    // data to share between different tests
    private static final AtomicReference<Account> ACCOUNT_1 = new AtomicReference<>();
    private static final AtomicReference<Account> ACCOUNT_2 = new AtomicReference<>();


    @Override
    protected ResourceConfig configure() {
        return super.configure().register(AccountEndpoint.class);
    }


    /**
     * Tests '/account' PUT.
     */
    @Test
    public void o1testCreate() throws IOException {
        final NewAccount newAccount = new NewAccount();
        testCreateValidations(this::testCreateValidation0, newAccount);

        newAccount.setOwnerName("Test Owner Name 1");
        newAccount.setComment("Test Comment 1");
        ACCOUNT_1.set(testCreate(newAccount, "Test Owner Name 1", "Test Comment 1"));
        assertTrue(ACCOUNT_1.get().getAccountId() > 0);

        newAccount.setOwnerName("Test Owner Name 2");
        newAccount.setComment(null);
        ACCOUNT_2.set(testCreate(newAccount, "Test Owner Name 2", null));
        assertTrue(ACCOUNT_2.get().getAccountId() > ACCOUNT_1.get().getAccountId());
    }

    private void testCreateValidations(final Consumer<String> operationInvocation, final NewAccount newAccount)
            throws JsonProcessingException {
        operationInvocation.accept("{\n" // broken json format
                + "  \"comment\"??? \"" + "Test Comment 1" + "\",\n"
                + "}");

        newAccount.setComment("Test Comment 1");
        operationInvocation.accept(getJson().writeValueAsString(newAccount));

        newAccount.setOwnerName(repeat('A', ACCOUNT.OWNER_NAME.getDataType().length() + 1));
        operationInvocation.accept(getJson().writeValueAsString(newAccount));

        newAccount.setOwnerName("Test Owner Name 1");
        newAccount.setComment(repeat('A', ACCOUNT.COMMENT.getDataType().length() + 1));
        operationInvocation.accept(getJson().writeValueAsString(newAccount));
    }

    private void testCreateValidation0(final String json) {
        testSimpleResponse(this::invokeCreate, json, BAD_REQUEST, TEXT_PLAIN_TYPE);
    }

    private Response invokeCreate(final String value) {
        return target().path("/account").request(APPLICATION_JSON_TYPE).put(Entity.json(value));
    }

    private Account testCreate(final NewAccount newAccount, final String ownerName, final String comment)
            throws IOException {
        final LocalDateTime intervalFrom = now(UTC).truncatedTo(MILLIS).minus(1, MILLIS);
        final Response resp = invokeCreate(getJson().writeValueAsString(newAccount));
        final LocalDateTime intervalTo = now(UTC).plus(1, MILLIS);

        assertEquals(OK, resp.getStatusInfo());
        assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType());
        final ByteArrayInputStream bais = (ByteArrayInputStream) resp.getEntity();
        final Account entity = getJson().readValue(bais, Account.class);
        assertAccount(entity, entity.getAccountId(), between(intervalFrom, intervalTo), ownerName, ZERO,
                between(intervalFrom, intervalTo), comment);
        return entity;
    }


    /**
     * Tests '/account/{id}' GET.
     */
    @Test
    public void o2testRead() throws IOException {
        testSimpleResponse(this::invokeRead, Long.toString(Long.MAX_VALUE), NO_CONTENT, null);
        testSimpleResponse(this::invokeRead, "A", NOT_FOUND, TEXT_PLAIN_TYPE);

        testReadResponse(ACCOUNT_1.get().getAccountId(), ACCOUNT_1.get());
        testReadResponse(ACCOUNT_2.get().getAccountId(), ACCOUNT_2.get());
    }

    private Response invokeRead(final String accountId) {
        return target().path("/account/" + accountId).request().get();
    }

    private void testReadResponse(
            final long accountId, final Account expected) throws IOException {

        final Response resp = invokeRead(Long.toString(accountId));
        assertEquals(OK, resp.getStatusInfo());
        assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType());
        final ByteArrayInputStream bais = (ByteArrayInputStream) resp.getEntity();
        final Account entity = getJson().readValue(bais, Account.class);
        assertAccount(entity, accountId,
                wideTo(expected.getCreated(), 2, MILLIS),
                expected.getOwnerName(), expected.getBalance(),
                wideTo(expected.getBalanceLastModified(), 2, MILLIS),
                expected.getComment());
    }

    private void assertAccount(final Account entity,
            final Long accountId,
            final Predicate<LocalDateTime> created,
            final String ownerName,
            final BigDecimal balance,
            final Predicate<LocalDateTime> balanceLastModified,
            final String comment) {

        assertEquals(accountId, entity.getAccountId());
        assertTrue(created.test(entity.getCreated()));
        assertEquals(ownerName, entity.getOwnerName());
        assertEquals(0, balance.compareTo(entity.getBalance()));
        assertTrue(balanceLastModified.test(entity.getBalanceLastModified()));
        assertEquals(comment, entity.getComment());
    }


    /**
     * Tests '/account' POST.
     */
    @Test
    public void o3testUpdate() throws IOException {
        // test 400  Incorrect request body
        final UpdateAccount updateAccount = new UpdateAccount();
        updateAccount.setAccountId(0L);
        testCreateValidations(this::testUpdateValidation, updateAccount);
        updateAccount.setAccountId(null);
        testUpdateValidation(getJson().writeValueAsString(updateAccount));

        // test 304  Account not exists
        updateAccount.setAccountId(Long.MAX_VALUE);
        updateAccount.setOwnerName("Updated Owner Name 1");
        updateAccount.setComment("Updated Comment 1");
        final Response resp0 = invokeUpdate(Entity.json(getJson().writeValueAsString(updateAccount)));
        assertEquals(NOT_MODIFIED, resp0.getStatusInfo());
        assertNull(resp0.getMediaType());

        testUpdate(ACCOUNT_1.get(), "Updated Owner Name 1", "Updated Comment 1");
        testUpdate(ACCOUNT_2.get(), "Updated Owner Name 2", "Updated Comment 2");
    }

    private void testUpdate(final Account initial, final String newOwnerName, final String newComment)
            throws IOException {
        final UpdateAccount updated = new UpdateAccount();
        updated.setAccountId(initial.getAccountId());
        updated.setOwnerName(newOwnerName);
        updated.setComment(newComment);
        final Response resp1 = invokeUpdate(Entity.json(getJson().writeValueAsString(updated)));
        assertEquals(OK, resp1.getStatusInfo());
        assertNull(resp1.getMediaType());

        final Account expected = new Account();
        expected.setAccountId(updated.getAccountId());
        expected.setCreated(initial.getCreated());
        expected.setOwnerName(updated.getOwnerName());
        expected.setBalance(initial.getBalance());
        expected.setBalanceLastModified(initial.getBalanceLastModified());
        expected.setComment(updated.getComment());
        testReadResponse(updated.getAccountId(), expected);
    }


    private void testUpdateValidation(final String json) {
        final Response resp = invokeUpdate(Entity.json(json));
        assertEquals(BAD_REQUEST, resp.getStatusInfo());
        assertEquals(TEXT_PLAIN_TYPE, resp.getMediaType());
    }

    private Response invokeUpdate(final Entity<String> value) {
        return target().path("/account").request().post(value);
    }


    /**
     * Tests '/account/{id}' DELETE.
     */
    @Test
    public void o4testDelete() {
        testSimpleResponse(this::invokeDelete, Long.toString(Long.MAX_VALUE), NOT_MODIFIED, null);
        testSimpleResponse(this::invokeDelete, "A", NOT_FOUND, TEXT_PLAIN_TYPE);

        testSimpleResponse(this::invokeDelete, ACCOUNT_1.get().getAccountId().toString(), OK, null);
        testSimpleResponse(this::invokeDelete, ACCOUNT_2.get().getAccountId().toString(), OK, null);
    }

    private Response invokeDelete(final String accountId) {
        return target().path("/account/" + accountId).request().delete();
    }


    @AfterClass
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static void afterClass() {
        ACCOUNT_1.set(null);
        ACCOUNT_2.set(null);
    }
}
