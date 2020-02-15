package com.github.edwgiz.sample.bank.core.webapp.commons;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DataType;
import org.jooq.Named;
import org.jooq.Record;
import org.jooq.TableField;

import javax.ws.rs.WebApplicationException;
import java.math.BigDecimal;

import static org.jooq.Nullability.NOT_NULL;
import static org.jooq.tools.StringUtils.toCamelCaseLC;

/**
 * Folds validation code, simplifying a building of messages with the Jooq-generated meta-data.
 */
public final class JooqAwareValidationUtils {


    static /* default */void appendName(final Named named, final StringBuilder msg) {
        msg.append(toCamelCaseLC(named.getName()));
    }

    /**
     * @param args arbitrary objects to be included into a message, the {@link TableField} will be translated
     *             accordingly its meta-data.
     * @return an exception wrapping the message.
     */
    public static WebApplicationException exception(final Object... args) {
        final StringBuilder msg = new StringBuilder();
        for (final Object arg : args) {
            if (arg instanceof TableField) {
                @SuppressWarnings("rawtypes") final TableField tblFld = (TableField) arg;
                appendName(tblFld, msg);
            } else {
                msg.append(arg);
            }
        }
        return ValidationUtils.exception(msg);
    }

    static /* default */void appendTableAndField(final TableField<?, ?> field, final StringBuilder msg) {
        appendName(field.getTable(), msg);
        msg.append('.');
        appendName(field, msg);
    }

    static /* default */WebApplicationException exception(final TableField<?, ?> field, final String checkMsg) {
        final StringBuilder msg = new StringBuilder();
        msg.append("'");
        appendTableAndField(field, msg);
        msg.append("' field ").append(checkMsg);
        return ValidationUtils.exception(msg);
    }


    /**
     * Check list <ol>
     * <li>value is not empty for non-nullable column;</li>
     * <li>value length doesn't exceeded a column length.</li></ol>
     *
     * @param field to build text reference to {@code field}
     * @param value value to check
     * @return value after all checks
     * @throws WebApplicationException thrown when a check of value vs field data type doesn't pass.
     */
    public static String checked(final TableField<?, String> field, final String value) throws WebApplicationException {
        final DataType<String> dataType = field.getDataType();
        if (StringUtils.isEmpty(value)) {
            if (dataType.nullability() == NOT_NULL) {
                throw exception(field, "must not be empty");
            }
        } else if (value.length() > dataType.length()) {
            throw exception(field, "must not exceed " + dataType.length() + " chars");
        }
        return value;
    }

    /**
     * @param field will be included into an exception message accordingly its meta-data.
     * @param value value to check.
     * @return original {@code value} as is.
     * @throws WebApplicationException thrown by failed check.
     */
    public static BigDecimal checkedPositive(final TableField<? extends Record, BigDecimal> field,
            final BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0L) {
            throw exception(field, "must be positive");
        }
        return value;
    }

    /**
     * @param field will be included into an exception message accordingly its meta-data.
     * @param value value to check.
     * @param <T> type of {@code value}
     * @return original {@code value} as is.
     * @throws WebApplicationException thrown by failed check.
     */
    public static <T> T checkedNotNull(final TableField<? extends Record, T> field, final T value) {
        if (value == null) {
            throw exception(field, "must not be null");
        }
        return value;
    }


    private JooqAwareValidationUtils() {
    }
}
