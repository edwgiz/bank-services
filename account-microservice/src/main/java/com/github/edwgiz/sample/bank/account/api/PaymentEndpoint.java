package com.github.edwgiz.sample.bank.account.api;

import com.github.edwgiz.sample.bank.account.jooq.tables.pojos.Payment;
import com.github.edwgiz.sample.bank.account.model.NewPayment;
import com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.InsertQuery;
import org.jooq.Record;
import org.jooq.UpdateQuery;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.edwgiz.sample.bank.account.jooq.Tables.ACCOUNT;
import static com.github.edwgiz.sample.bank.account.jooq.Tables.PAYMENT;
import static com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils.checked;
import static com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils.checkedNotNull;
import static com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils.checkedPositive;
import static com.github.edwgiz.sample.bank.core.webapp.commons.ValidationUtils.exception;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.BAD_REQUEST_CODE;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.CONFLICT_CODE;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.OK_CODE;
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.jooq.impl.DSL.row;


@Path("/payment")
@Singleton
public final class PaymentEndpoint {

    private final DSLContext dslCtx;

    /**
     * @param value a ready-to-use DSL context
     */
    @Inject
    public PaymentEndpoint(final DSLContext value) {
        this.dslCtx = value;
    }


    @PUT
    @Consumes(APPLICATION_JSON)
    @Operation(description = "Creates new payment", responses = {
            @ApiResponse(responseCode = OK_CODE, description = "Data successfully saved",
                    content = @Content(mediaType = TEXT_PLAIN,
                            schema = @Schema(ref = "#/components/schemas/Payment/properties/paymentId"))),
            @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Request body does not match Payment schema",
                    content = @Content(mediaType = TEXT_PLAIN)),
            @ApiResponse(responseCode = CONFLICT_CODE, description = "Account is locked due to other operation",
                    content = @Content(mediaType = TEXT_PLAIN))
    })
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public Response create(@RequestBody(content = @Content(mediaType = APPLICATION_JSON, examples = @ExampleObject(
            value = /*language=JSON*/ "{\n"
                    + "  \"amount\": 10000,\n"
                    + "  \"withdrawalAccountId\": 0,\n"
                    + "  \"depositAccountId\": 2,\n"
                    + "  \"comment\": \"Donation\"\n"
                    + "}", ref = "#/components/schemas/NewPayment"))) final NewPayment newPayment) {

        final LocalDateTime now = now(UTC);
        final Payment payment = new Payment(null, now,
                checkedPositive(PAYMENT.AMOUNT, newPayment.getAmount()),
                checkedNotNull(PAYMENT.WITHDRAWAL_ACCOUNT_ID, newPayment.getWithdrawalAccountId()),
                checkedNotNull(PAYMENT.DEPOSIT_ACCOUNT_ID, newPayment.getDepositAccountId()),
                checked(PAYMENT.COMMENT, newPayment.getComment()));

        if (payment.getWithdrawalAccountId().equals(payment.getDepositAccountId())) {
            throw JooqAwareValidationUtils.exception(PAYMENT.WITHDRAWAL_ACCOUNT_ID,
                    " and ", PAYMENT.DEPOSIT_ACCOUNT_ID,
                    " must reference to different accounts");
        }

        return dslCtx.transactionResult(cnf -> {
            final BigDecimal withdrawalBalance;
            final BigDecimal depositBalance;
            if (payment.getWithdrawalAccountId() < payment.getDepositAccountId()) {
                withdrawalBalance = getWithdrawalBalanceExclusively(cnf, payment.getWithdrawalAccountId());
                depositBalance = getDepositBalanceExclusively(cnf, payment.getDepositAccountId());
            } else {
                // reorder to avoid a deadlock
                depositBalance = getDepositBalanceExclusively(cnf, payment.getDepositAccountId());
                withdrawalBalance = getWithdrawalBalanceExclusively(cnf, payment.getWithdrawalAccountId());
            }
            if (withdrawalBalance.compareTo(payment.getAmount()) < 0) {
                throw exception("Insufficient withdrawal balance");
            }

            final Long paymentId = insertPayment(payment, cnf);

            updateAccount(cnf, payment.getWithdrawalAccountId(), withdrawalBalance.subtract(payment.getAmount()),
                    now, "withdraw");
            updateAccount(cnf, payment.getDepositAccountId(), depositBalance.add(payment.getAmount()),
                    now, "deposit");
            return Response.ok(Long.toString(paymentId)).type(TEXT_PLAIN_TYPE).build();
        });
    }

    /* default */BigDecimal getWithdrawalBalanceExclusively(final Configuration cnf, final long accountId) {
        return getBalanceExclusively(accountId, cnf, "Withdrawal");
    }

    /* default */BigDecimal getDepositBalanceExclusively(final Configuration cnf, final long accountId) {
        return getBalanceExclusively(accountId, cnf, "Deposit");
    }

    /* default */BigDecimal getBalanceExclusively(final long accountId, final Configuration cnf,
            final String accountSide) throws WebApplicationException {
        try {
            final BigDecimal balance = cnf.dsl()
                    .select(ACCOUNT.BALANCE, ACCOUNT.BALANCE_LAST_MODIFIED).from(ACCOUNT)
                    .where(ACCOUNT.ACCOUNT_ID.eq(accountId)).forUpdate()
                    .fetchAny(ACCOUNT.BALANCE);
            if (balance == null) {
                throw exception(new StringBuffer()
                        .append(accountSide).append(" account not exists by accountId=").append(accountId));
            }
            return balance;
        } catch (DataAccessException ex) {
            throw exception(CONFLICT, new StringBuffer()
                    .append(accountSide).append(" account with accountId=").append(accountId)
                    .append(" is temporary locked due to another payment or other operation"));
        }
    }

    /* default */Long insertPayment(final Payment values, final Configuration cnf) {
        final InsertQuery<Record> insert = cnf.dsl().insertQuery(PAYMENT);
        insert.setRecord(cnf.dsl().newRecord(PAYMENT, values));
        insert.setReturning(PAYMENT.PAYMENT_ID);
        insertPaymentCheckUpdatedRows(insert.execute());
        return insert.getResult().getValue(0, PAYMENT.PAYMENT_ID);
    }

    /* default */void insertPaymentCheckUpdatedRows(final int rows) {
        if (rows != 1) {
            throw new IllegalStateException("Can't create payment record");
        }
    }

    /* default */void updateAccount(final Configuration cnf, final long accountId, final BigDecimal balance,
            final LocalDateTime processingDatetime, final String accountSide) {
        final UpdateQuery<?> update = cnf.dsl().updateQuery(ACCOUNT);
        update.addConditions(ACCOUNT.ACCOUNT_ID.eq(accountId));
        update.addValues(row(ACCOUNT.BALANCE, ACCOUNT.BALANCE_LAST_MODIFIED), row(balance, processingDatetime));
        final int rows = update.execute();
        updateAccountCheckUpdatedRows(rows, accountSide);
    }

    /* default */void updateAccountCheckUpdatedRows(final int rows, final String accountSide) {
        if (rows != 1) {
            throw new IllegalStateException("Can't update balance of " + accountSide + " account");
        }
    }


    @GET
    @Produces(APPLICATION_JSON)
    @Operation(description = "Returns filtered payments sorted by processed date", responses = {
            @ApiResponse(responseCode = OK_CODE,
                    content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                    @Schema(ref = "#/components/schemas/Payment")))),
            @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Incorrect request parameters",
                    content = @Content(mediaType = TEXT_PLAIN)),
    })
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<Payment> list(
            @QueryParam("accountId") @Parameter(in = QUERY, required = true, example = "2",
                    description = "Unique Identifier, picks up both, withdrawal and deposit accounts")
            final Long accountId,
            @QueryParam("from") @Parameter(in = QUERY, name = "from", required = true,
                    example = "2020-02-02T00:00:00.000", description = "Processed date-time from, UTC, inclusive")
            final LocalDateTime intervalFrom,
            @QueryParam("to") @Parameter(in = QUERY, name = "to", required = true,
                    example = "2020-03-02T00:00:00.000", description = "Processed date-time to, UTC, inclusive")
            final LocalDateTime intervalTo) {

        if (accountId == null) {
            throw exception("'accountId' is undefined");
        }
        if (intervalFrom.compareTo(intervalTo) > 0) {
            throw exception("'from' must not be after 'to'");
        }

        return dslCtx.transactionResult(cnf -> cnf.dsl()
                .selectFrom(PAYMENT)
                .where(PAYMENT.PROCESSED.between(intervalFrom, intervalTo)
                        .and(PAYMENT.WITHDRAWAL_ACCOUNT_ID.eq(accountId)
                                .or(PAYMENT.DEPOSIT_ACCOUNT_ID.eq(accountId))))
                .orderBy(PAYMENT.PROCESSED)
                .fetchInto(Payment.class));
    }
}
