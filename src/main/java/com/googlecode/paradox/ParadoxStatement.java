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

import com.googlecode.paradox.exceptions.*;
import com.googlecode.paradox.planner.context.Context;
import com.googlecode.paradox.planner.context.SelectContext;
import com.googlecode.paradox.planner.plan.Plan;
import com.googlecode.paradox.planner.plan.SelectPlan;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.utils.Constants;
import com.googlecode.paradox.utils.Utils;

import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * JDBC statement implementation.
 *
 * @version 1.9
 * @since 1.0
 */
@SuppressWarnings({"java:S1448", "java:S1200"})
class ParadoxStatement implements Statement {

    /**
     * Statement list.
     */
    protected final List<Plan<?, ?>> statements = new ArrayList<>();
    /**
     * ResultSet list.
     */
    protected final List<ResultSet> resultSets = new ArrayList<>();
    /**
     * Result set concurrency.
     */
    protected final int resultSetConcurrency;
    /**
     * Result set type.
     */
    protected final int resultSetType;
    /**
     * Result set holdability.
     */
    private final int resultSetHoldability;
    /**
     * This statement active executions list.
     */
    private final HashSet<Context> activeExecutions = new HashSet<>();
    /**
     * The Paradox connection.
     */
    protected final WeakReference<ParadoxConnection> connection;
    /**
     * Auto generated keys.
     */
    protected int autoGeneratedKeys;
    /**
     * Result set index.
     */
    protected int resultSetIndex = -1;
    /**
     * Close on completion.
     */
    protected boolean closeOnCompletion;
    /**
     * If this statement is closed.
     */
    protected boolean closed;
    /**
     * The max rows.
     */
    protected int maxRows;
    /**
     * The fetch direction.
     */
    private int fetchDirection = ResultSet.FETCH_FORWARD;
    /**
     * The fetch size.
     */
    private int fetchSize = 10;
    /**
     * The max field size.
     */
    private int maxFieldSize = Constants.MAX_STRING_SIZE;
    /**
     * If this statement is pool capable.
     */
    private boolean poolable;
    /**
     * The query timeout.
     */
    private int queryTimeout;
    /**
     * The connection information.
     */
    protected ConnectionInfo connectionInfo;

    /**
     * Creates a statement.
     *
     * @param connection           the Paradox connection.
     * @param resultSetConcurrency the result set concurrency.
     * @param resultSetHoldability the result set holdability.
     * @param resultSetType        the result set type.
     */
    ParadoxStatement(final ParadoxConnection connection, final int resultSetType, final int resultSetConcurrency,
                     final int resultSetHoldability) {
        this.connection = new WeakReference<>(connection);
        this.connectionInfo = connection.getConnectionInfo();
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.autoGeneratedKeys = Statement.NO_GENERATED_KEYS;
    }

    protected int[] executeStatements() throws SQLException {
        final ArrayList<Integer> ret = new ArrayList<>();
        // One for statement.
        for (final Plan<?, ?> plan : statements) {
            try {
                ret.addAll(executeStatement(plan, null, null));
            } catch (@SuppressWarnings("java:S1166") final InternalException e) {
                throw e.getCause();
            }

        }

        return ret.stream().mapToInt(Integer::intValue).toArray();
    }

    @SuppressWarnings("java:S2093")
    protected List<Integer> executeStatement(final Plan<?, ?> plan, final Object[] params, final ParadoxType[] types)
            throws SQLException {
        ArrayList<Integer> ret = new ArrayList<>();
        if (plan instanceof SelectPlan) {
            final SelectPlan selectPlan = (SelectPlan) plan;
            final SelectContext context = selectPlan.createContext(this.connectionInfo, params, types);
            context.setMaxRows(maxRows);
            activeExecutions.add(context);

            try {
                final List<Object[]> values = selectPlan.execute(context);

                final ParadoxResultSet resultSet = new ParadoxResultSet(this.connectionInfo, this, values,
                        selectPlan.getColumns());
                resultSet.setFetchDirection(ResultSet.FETCH_FORWARD);
                resultSet.setType(resultSetType);
                resultSet.setConcurrency(resultSetConcurrency);
                ret.add(Statement.SUCCESS_NO_INFO);
                resultSets.add(resultSet);
            } finally {
                activeExecutions.remove(context);
            }
        }

        return ret;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void addBatch(final String sql) throws SQLException {
        if (connection == null || connection.get() == null) {
            throw new ParadoxConnectionException(ParadoxConnectionException.Error.NOT_CONNECTED);
        }

        this.statements.add(Objects.requireNonNull(connection.get()).createPlan(sql));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void cancel() throws SQLFeatureNotSupportedException {
        for (final Context node : activeExecutions) {
            node.cancel();
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void clearBatch() {
        this.statements.clear();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void clearWarnings() {
        // Not used.
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void close() throws SQLException {
        for (final ResultSet rs : resultSets) {
            rs.close();
        }

        this.resultSets.clear();
        this.statements.clear();

        this.closed = true;
        this.connectionInfo = null;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void closeOnCompletion() {
        this.closeOnCompletion = true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean execute(final String sql) throws SQLException {
        if (connection == null || connection.get() == null) {
            throw new ParadoxConnectionException(ParadoxConnectionException.Error.NOT_CONNECTED);
        }

        this.statements.add(Objects.requireNonNull(connection.get()).createPlan(sql));

        executeStatements();

        return getMoreResults();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int[] executeBatch() throws SQLException {
        return executeStatements();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        if (connection == null || connection.get() == null) {
            throw new ParadoxConnectionException(ParadoxConnectionException.Error.NOT_CONNECTED);
        }

        this.statements.add(Objects.requireNonNull(connection.get()).createPlan(sql));

        executeStatements();

        if (getMoreResults()) {
            return getResultSet();
        }

        throw new ParadoxSyntaxErrorException(SyntaxError.INVALID_SELECT_STATEMENT);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int executeUpdate(final String sql) throws SQLException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) {
        return 0;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int executeUpdate(final String sql, final int[] columnIndexes) {
        return 0;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int executeUpdate(final String sql, final String[] columnNames) {
        return 0;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Connection getConnection() {
        if (connection != null) {
            return this.connection.get();
        }

        return null;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            return resultSet.getFetchDirection();
        }

        return fetchDirection;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            resultSet.setFetchDirection(direction);
        }

        this.fetchDirection = direction;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getFetchSize() throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            return resultSet.getFetchSize();
        }

        return fetchSize;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setFetchSize(int fetchSize) throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            resultSet.setFetchSize(fetchSize);
        }

        this.fetchSize = fetchSize;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public ResultSet getGeneratedKeys() {
        return new ParadoxResultSet(this.connectionInfo, this, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getMaxFieldSize() {
        return this.maxFieldSize;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setMaxFieldSize(final int max) throws SQLException {
        if (max > Constants.MAX_STRING_SIZE) {
            throw new ParadoxDataException(DataError.INVALID_FIELD_SIZE);
        }

        this.maxFieldSize = max;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getMaxRows() {
        return this.maxRows;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setMaxRows(final int max) {
        this.maxRows = max;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean getMoreResults() {
        if (resultSetIndex < resultSets.size()) {
            resultSetIndex++;
        }

        return resultSetIndex < resultSets.size();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        switch (current) {
            case Statement.CLOSE_CURRENT_RESULT:
                ResultSet currentResult = getResultSet();
                if (currentResult != null) {
                    currentResult.close();
                }
                break;

            case Statement.CLOSE_ALL_RESULTS:
                for (final ResultSet rs : resultSets) {
                    rs.close();
                }
                break;

            default:
                // Do nothing.
                break;
        }

        return getMoreResults();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getQueryTimeout() {
        return this.queryTimeout;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setQueryTimeout(final int seconds) {
        this.queryTimeout = seconds;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public ResultSet getResultSet() {
        if (resultSetIndex != -1 && resultSetIndex < resultSets.size()) {
            return resultSets.get(resultSetIndex);
        }

        return null;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            return resultSet.getConcurrency();
        }

        return resultSetConcurrency;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getResultSetHoldability() {
        return resultSetHoldability;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getResultSetType() throws SQLException {
        final ResultSet resultSet = getResultSet();
        if (resultSet != null) {
            return resultSet.getType();
        }

        return resultSetType;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public int getUpdateCount() {
        return -1;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isCloseOnCompletion() {
        return closeOnCompletion;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isPoolable() {
        return this.poolable;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setPoolable(final boolean canPool) {
        this.poolable = canPool;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isWrapperFor(final Class<?> iFace) {
        return Utils.isWrapperFor(this, iFace);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setEscapeProcessing(final boolean enable) {
        // Nothing to do here.
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T> T unwrap(final Class<T> iFace) throws SQLException {
        return Utils.unwrap(this, iFace);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void setCursorName(final String name) throws ParadoxNotSupportedException {
        throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED);
    }
}
