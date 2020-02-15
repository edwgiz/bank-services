package com.github.edwgiz.sample.bank.account.api;

import com.github.edwgiz.sample.bank.account.jooq.tables.pojos.Account;
import com.github.edwgiz.sample.bank.account.model.NewAccount;
import com.github.edwgiz.sample.bank.account.model.UpdateAccount;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.DSLContext;
import org.jooq.InsertQuery;
import org.jooq.Record;
import org.jooq.UpdateQuery;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

import static com.github.edwgiz.sample.bank.account.jooq.Tables.ACCOUNT;
import static com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils.checked;
import static com.github.edwgiz.sample.bank.core.webapp.commons.JooqAwareValidationUtils.checkedNotNull;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.BAD_REQUEST_CODE;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.NOT_MODIFIED_CODE;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.NO_CONTENT_CODE;
import static com.github.edwgiz.sample.bank.core.webapp.http.ResponseUtils.OK_CODE;
import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH;
import static java.math.BigDecimal.ZERO;
import static java.time.ZoneOffset.UTC;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.jooq.impl.DSL.row;


@OpenAPIDefinition(
        info = @Info(
                title = "Bank Accounts",
                version = "1.0.0",
                description = "Bank Account micro-service",
                termsOfService = "Demo",
                contact = @Contact(email = "edwgiz@gmail.com"),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                )
        )
)
@Path("/account")
@Singleton
public final class AccountEndpoint {

    private final DSLContext dslCtx;

    /**
     * @param value a ready-to-use DSL context
     */
    @Inject
    public AccountEndpoint(final DSLContext value) {
        this.dslCtx = value;
    }


    @PUT
    @Operation(description = "Creates new account with a zero balance", responses = {
            @ApiResponse(responseCode = OK_CODE, description = "Data successfully saved",
                    content = @Content(mediaType = APPLICATION_JSON, schema =
                    @Schema(ref = "#/components/schemas/Account"))),
            @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Request body does not match Account schema",
                    content = @Content(mediaType = TEXT_PLAIN))})
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public Response create(
            @RequestBody(content = @Content(mediaType = APPLICATION_JSON, examples = @ExampleObject(
                    /* language=JSON */ "{\n"
                    + "  \"ownerName\": \"Donald Duck\",\n"
                    + "  \"comment\": \"Cartoon Character\"\n"
                    + "}"), schema = @Schema(ref = "#/components/schemas/NewAccount"))) final NewAccount newAccount) {

        final LocalDateTime now = LocalDateTime.now(UTC);
        final Account account = new Account(null, now,
                checked(ACCOUNT.OWNER_NAME, newAccount.getOwnerName()),
                ZERO, now,
                checked(ACCOUNT.COMMENT, newAccount.getComment()));

        account.setAccountId(dslCtx.transactionResult(cnf -> {
            final InsertQuery<Record> insert = cnf.dsl().insertQuery(ACCOUNT);
            insert.setRecord(cnf.dsl().newRecord(ACCOUNT, account));
            insert.setReturning(ACCOUNT.ACCOUNT_ID);
            insert.execute();
            return insert.getResult().getValue(0, ACCOUNT.ACCOUNT_ID); // workaround to receive generated id
        }));
        return Response.ok(account, APPLICATION_JSON_TYPE).build();
    }


    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    @Operation(description = "Returns account by its unique identifier", responses = {
            @ApiResponse(responseCode = OK_CODE, description = "Account exists", ref = "#/components/schemas/Account"),
            @ApiResponse(responseCode = NO_CONTENT_CODE, description = "Account not exists")
    })
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public Account read(@PathParam("id") @Parameter(in = PATH, description = "Unique identifier", required = true,
            example = "2") final long accountId) {

        return dslCtx.transactionResult(cnf -> cnf.dsl()
                .selectFrom(ACCOUNT).where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .fetchAnyInto(Account.class));
    }


    @POST
    @Operation(description = "Update account", responses = {
            @ApiResponse(responseCode = OK_CODE, description = "Account successfully updated"),
            @ApiResponse(responseCode = NOT_MODIFIED_CODE, description = "Account not exists"),
            @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Incorrect request body")
    })
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public Response update(@RequestBody(content = @Content(mediaType = APPLICATION_JSON, examples = @ExampleObject(
            /*language=JSON */ "{\n"
            + "  \"accountId\": 2,\n"
            + "  \"ownerName\": \"Donald Duck\",\n"
            + "  \"comment\": \"Cartoon Character\"\n"
            + "}"), schema = @Schema(ref = "#/components/schemas/UpdateAccount"))) final UpdateAccount account) {

        final int rows = dslCtx.transactionResult(cnf -> {
            final UpdateQuery<?> update = cnf.dsl().updateQuery(ACCOUNT);
            update.addValues(row(ACCOUNT.OWNER_NAME, ACCOUNT.COMMENT), row(
                    checked(ACCOUNT.OWNER_NAME, account.getOwnerName()),
                    checked(ACCOUNT.COMMENT, account.getComment())));
            update.addConditions(ACCOUNT.ACCOUNT_ID.eq(checkedNotNull(ACCOUNT.ACCOUNT_ID, account.getAccountId())));
            return update.execute();
        });

        return Response.status(rows == 1 ? OK : NOT_MODIFIED).build();
    }


    @DELETE
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    @Operation(description = "Returns account by its unique identifier", responses = {
            @ApiResponse(responseCode = OK_CODE, description = "Account deleted"),
            @ApiResponse(responseCode = NOT_MODIFIED_CODE, description = "Account not exists")})
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public Response delete(@PathParam("id") @Parameter(in = PATH, description = "Unique identifier", required = true,
            example = "2") final long accountId) {

        final int rows = dslCtx.transactionResult(cnf -> cnf.dsl()
                .delete(ACCOUNT).where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .execute());

        return Response.status(rows == 1 ? OK : NOT_MODIFIED).build();
    }
}
