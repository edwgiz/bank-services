package com.github.edwgiz.sample.bank.core.webapp.commons;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DataType;
import org.jooq.Named;
import org.jooq.Nullability;
import org.jooq.Table;
import org.jooq.TableField;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JooqAwareValidationUtilsTest {

    private <T> TableField<?, T> mockTableField(final String fieldName) {
        @SuppressWarnings("unchecked") final TableField<?, T> tblFld = Mockito.mock(TableField.class);
        Mockito.doReturn(fieldName).when(tblFld).getName();
        return tblFld;
    }

    @Test
    public void testAppendName() {
        final Named named = Mockito.mock(Named.class);
        testAppendName(named, "to_CAmEL_case", "toCamelCase");
        testAppendName(named, "To_camel_casE_", "toCamelCase_");
        testAppendName(named, "_TO_CAMEL_CASE", "_ToCamelCase");
    }

    @Test
    public void testFailWithVarargs() {
        final TableField<?, ?> tf1 = mockTableField("COLUMN_1");
        final TableField<?, ?> tf2 = mockTableField("COLUMN_2");
        ValidationUtilsTest.assertWebApplicationException(
                JooqAwareValidationUtils.exception("Inconsistent data for ", tf1, " and ", tf2),
                "Inconsistent data for column_1 and column_2");
    }


    private void testAppendName(final Named named, final String givenName, final String expectedResult) {
        Mockito.reset(named);
        Mockito.doReturn(givenName).when(named).getName();
        final StringBuilder msg = new StringBuilder();
        JooqAwareValidationUtils.appendName(named, msg);
        assertEquals(expectedResult, msg.toString());
        Mockito.verify(named).getName();
        Mockito.verifyNoMoreInteractions(named);
    }

    private <T> TableField<?, T> mockTableTableFieldAndTable(final String tableName, final String fieldName) {
        final TableField<?, T> tblFld = mockTableField(fieldName);
        final Table<?> tbl = Mockito.mock(Table.class);
        Mockito.doReturn(tbl).when(tblFld).getTable();
        Mockito.doReturn(tableName).when(tbl).getName();
        return tblFld;
    }

    @Test
    public void testAppendTableAndField() {
        final TableField<?, ?> tblFld = mockTableTableFieldAndTable("TEST_TABLE", "TEST_COLUMN");
        final StringBuilder msg = new StringBuilder();
        JooqAwareValidationUtils.appendTableAndField(tblFld, msg);
        assertEquals("testTable.testColumn", msg.toString());
    }

    @Test
    public void testExceptionWithTableField() {
        final TableField<?, ?> tblFld = mockTableTableFieldAndTable("TABLE_TEST", "COLUMN_3");
        final WebApplicationException wae = JooqAwareValidationUtils.exception(tblFld, "should no be empty");
        ValidationUtilsTest.assertWebApplicationException(wae, "'tableTest.column_3' field should no be empty");
    }


    private void testCheckedWithException(final TableField<?, String> tblFld, final DataType<?> dataType,
            final String expectedMsg, final String givenValue, final Nullability nullability, final int maxLength) {
        try {
            testChecked(tblFld, dataType, givenValue, nullability, maxLength);
        } catch (WebApplicationException ex) {
            ValidationUtilsTest.assertWebApplicationException(ex, expectedMsg);
        }
    }

    private void testCheckedWhenEmpty(final TableField<?, String> tblFld, final DataType<?> dataType,
            final String givenValue) {
        assertEquals(givenValue, testChecked(tblFld, dataType, givenValue, Nullability.DEFAULT, Integer.MAX_VALUE));
        assertEquals(givenValue, testChecked(tblFld, dataType, givenValue, Nullability.NULL, Integer.MAX_VALUE));
        testCheckedWithException(tblFld, dataType, "'tableTest4.col4' field must not be empty",
                givenValue, Nullability.NOT_NULL, Integer.MAX_VALUE);
    }

    private String testChecked(final TableField<?, String> tblFld, final DataType<?> dataType,
            final String givenValue, final Nullability nullability, final int maxLength) {
        //noinspection unchecked
        Mockito.reset(dataType);
        Mockito.doReturn(dataType).when(tblFld).getDataType();
        Mockito.doReturn(nullability).when(dataType).nullability();
        Mockito.doReturn(maxLength).when(dataType).length();
        return JooqAwareValidationUtils.checked(tblFld, givenValue);
    }

    @Test
    public void testChecked() {
        final TableField<?, String> tblFld = mockTableTableFieldAndTable("table_test4", "col4");
        final DataType<?> dataType = Mockito.mock(DataType.class);
        testCheckedWhenEmpty(tblFld, dataType, null);
        testCheckedWhenEmpty(tblFld, dataType, "");

        final int maxLength = 7;
        final String acceptableValue = StringUtils.repeat('A', maxLength);
        assertEquals(acceptableValue, testChecked(tblFld, dataType, acceptableValue, null, maxLength));
        final String tooLongValue = acceptableValue + "B";
        testCheckedWithException(tblFld, dataType, "'tableTest4.col4' field must not exceed 7 chars",
                tooLongValue, null, maxLength);
    }

    @Test
    public void checkedPositive() {
        final TableField<?, BigDecimal> tblFld = mockTableTableFieldAndTable("table_test5", "col5");
        final String expectedMsg = "'tableTest5.col5' field must be positive";
        try {
            JooqAwareValidationUtils.checkedPositive(tblFld, null);
        } catch (WebApplicationException ex) {
            ValidationUtilsTest.assertWebApplicationException(ex, expectedMsg);
        }
        try {
            JooqAwareValidationUtils.checkedPositive(tblFld, BigDecimal.ZERO);
        } catch (WebApplicationException ex) {
            ValidationUtilsTest.assertWebApplicationException(ex, expectedMsg);
        }
        assertEquals(BigDecimal.ONE, JooqAwareValidationUtils.checkedPositive(tblFld, BigDecimal.ONE));
        assertEquals(BigDecimal.TEN, JooqAwareValidationUtils.checkedPositive(tblFld, BigDecimal.TEN));
    }

    @Test
    public void checkedNotNull() {
        final TableField<?, Object> tblFld = mockTableTableFieldAndTable("table_test6", "col6");
        final String expectedMsg = "'tableTest6.col6' field must not be null";
        try {
            JooqAwareValidationUtils.checkedNotNull(tblFld, null);
        } catch (WebApplicationException wae) {
            ValidationUtilsTest.assertWebApplicationException(wae, expectedMsg);
        }
        final Object obj = new Object();
        assertEquals(obj, JooqAwareValidationUtils.checkedNotNull(tblFld, obj));
    }
}
