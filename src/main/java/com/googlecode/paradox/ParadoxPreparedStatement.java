/*
 * Copyright (C) 2009 Leonardo Alves da Costa
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.googlecode.paradox;

import com.googlecode.paradox.exceptions.ParadoxException;
import com.googlecode.paradox.exceptions.ParadoxNotSupportedException;
import com.googlecode.paradox.exceptions.ParadoxSyntaxErrorException;
import com.googlecode.paradox.parser.SQLParser;
import com.googlecode.paradox.parser.nodes.StatementNode;
import com.googlecode.paradox.planner.Planner;
import com.googlecode.paradox.planner.plan.Plan;
import com.googlecode.paradox.planner.plan.SelectPlan;
import com.googlecode.paradox.utils.Utils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * PreparedStatement implementation class.
 *
 * @version 1.0
 * @since 1.6.0
 */
class ParadoxPreparedStatement extends ParadoxStatement implements PreparedStatement {

    /**
     * Execution list.
     */
    protected final List<Object[]> executions = new ArrayList<>();
    /**
     * Parameter list.
     */
    private final Object[] currentParameterValues;

    ParadoxPreparedStatement(final ParadoxConnection connection, final String sql, final int resultSetType,
                             final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
        super(connection, resultSetType, resultSetConcurrency, resultSetHoldability);

        final SQLParser parser = new SQLParser(connection, sql);
        statements.addAll(parser.parse());

        if (statements.isEmpty()) {
            throw new ParadoxSyntaxErrorException(ParadoxSyntaxErrorException.Error.EMPTY_SQL);
        } else if (statements.size() > 1) {
            throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.USE_BATCH_OPERATION);
        }

        currentParameterValues = new Object[statements.get(0).getParameterCount()];
    }

    public void setAutoGeneratedKeys(int autoGeneratedKeys) {
        this.autoGeneratedKeys = autoGeneratedKeys;
    }

    @Override
    protected int[] executeStatements() throws SQLException {
        // Close all existing result sets.
        for (final ResultSet rs : resultSets) {
            rs.close();
        }

        resultSets.clear();

        ArrayList<Integer> ret = new ArrayList<>();
        // One for statement.
        for (final StatementNode statement : statements) {
            final Plan plan = Planner.create(connection, statement);

            // One for parameters.
            for (final Object[] params : executions) {
                // FIXME use parameterList.
                plan.execute(this.connection, maxRows);

                if (plan instanceof SelectPlan) {
                    final ParadoxResultSet resultSet = new ParadoxResultSet(this.connection, this,
                            ((SelectPlan) plan).getValues(), ((SelectPlan) plan).getColumns());
                    resultSet.setFetchDirection(ResultSet.FETCH_FORWARD);
                    resultSet.setType(resultSetType);
                    resultSet.setConcurrency(resultSetConcurrency);
                    ret.add(Statement.SUCCESS_NO_INFO);
                    resultSets.add(resultSet);
                }
            }
        }

        int[] values = new int[ret.size()];
        for (int loop = 0; loop < ret.size(); loop++) {
            values[loop] = ret.get(loop);
        }
        return values;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        executions.add(currentParameterValues);

        executeStatements();

        resultSetIndex = 0;
        return getResultSet();
    }

    @Override
    public int executeUpdate() throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = null;
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    /**
     * {@inheritDoc}.
     *
     * @deprecated to keep compatibility.
     */
    @Deprecated
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void clearParameters() {
        Arrays.fill(currentParameterValues, null);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public boolean execute() throws SQLException {
        // FIXME more generic.
        return executeQuery() != null;
    }

    @Override
    public void addBatch() {
        executions.add(currentParameterValues);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        final ResultSet current = getResultSet();
        if (current != null) {
            return current.getMetaData();
        }

        return null;
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        checkIndex(parameterIndex);
        currentParameterValues[parameterIndex - 1] = x;
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public void addBatch(final String sql) throws SQLException {
        final SQLParser parser = new SQLParser(connection, sql);
        final List<StatementNode> batchStatements = parser.parse();

        for (final StatementNode statement : batchStatements) {
            if (statement.getParameterCount() != currentParameterValues.length) {
                throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.INCONSISTENT_PARAMETER_LIST);
            }
        }

        this.statements.addAll(batchStatements);
    }

    @Override
    public void clearBatch() {
        this.executions.clear();
        while (this.statements.size() > 1) {
            this.statements.remove(1);
        }
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    @Override
    public <T> T unwrap(Class<T> iFace) throws SQLException {
        return Utils.unwrap(this, iFace);
    }

    @Override
    public boolean isWrapperFor(Class<?> iFace) {
        return Utils.isWrapperFor(this, iFace);
    }

    private void checkIndex(final int index) throws ParadoxException {
        if (index < 1 || index > currentParameterValues.length) {
            throw new ParadoxException(ParadoxException.Error.INVALID_COLUMN_INDEX, Integer.toString(index), null);
        }
    }
}
