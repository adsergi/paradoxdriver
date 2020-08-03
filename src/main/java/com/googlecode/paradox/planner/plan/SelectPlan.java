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
package com.googlecode.paradox.planner.plan;

import com.googlecode.paradox.ConnectionInfo;
import com.googlecode.paradox.data.TableData;
import com.googlecode.paradox.exceptions.*;
import com.googlecode.paradox.metadata.ParadoxDataFile;
import com.googlecode.paradox.metadata.ParadoxField;
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.parser.nodes.*;
import com.googlecode.paradox.planner.FieldValueUtils;
import com.googlecode.paradox.planner.nodes.*;
import com.googlecode.paradox.planner.nodes.join.ANDNode;
import com.googlecode.paradox.planner.nodes.join.AbstractJoinNode;
import com.googlecode.paradox.planner.nodes.join.ORNode;
import com.googlecode.paradox.planner.sorting.OrderByComparator;
import com.googlecode.paradox.planner.sorting.OrderType;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.rowset.ValuesConverter;
import com.googlecode.paradox.utils.FunctionalUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlecode.paradox.utils.FunctionalUtils.functionWrapper;
import static com.googlecode.paradox.utils.FunctionalUtils.predicateWrapper;

/**
 * Creates a SELECT plan for execution.
 *
 * @version 1.14
 * @since 1.1
 */
@SuppressWarnings({"java:S1448", "java:S1200"})
public final class SelectPlan implements Plan<List<Object[]>> {

    /**
     * The columns in this plan to show in result set.
     */
    private final List<Column> columns = new ArrayList<>();
    /**
     * The columns to load in this plan, not only in result set.
     */
    private final List<Column> columnsFromFunctions = new ArrayList<>();
    /**
     * The tables in this plan.
     */
    private final List<PlanTableNode> tables;
    /**
     * If this result needs to be distinct.
     */
    private final boolean distinct;
    /**
     * Group by fields.
     */
    private final List<Column> groupByFields = new ArrayList<>();
    /**
     * Order by fields.
     */
    private final List<Column> orderByFields = new ArrayList<>();
    /**
     * Order type.
     */
    private final List<OrderType> orderTypes = new ArrayList<>();
    /**
     * The data values.
     */
    private List<Object[]> values = Collections.emptyList();
    /**
     * The conditions to filter values
     */
    private AbstractConditionalNode condition;
    /**
     * If this statement was cancelled.
     * <p>
     * FIXME move to execution context.
     */
    private boolean cancelled;
    /**
     * Table joiner.
     */
    private final TableJoiner joiner = new TableJoiner();
    /**
     * Grouping columns.
     */
    private int[] groupFunctionColumns;
    /**
     * Grouping columns.
     */
    private int[] groupColumns;
    /**
     * Is this plan is has a group by.
     */
    private final boolean groupBy;

    /**
     * Creates a SELECT plan.
     *
     * @param connectionInfo the connection info.
     * @param statement      the statement node.
     * @throws ParadoxSyntaxErrorException in case of failures.
     */
    public SelectPlan(final ConnectionInfo connectionInfo, final SelectNode statement) throws SQLException {
        this.condition = statement.getCondition();
        this.distinct = statement.isDistinct();

        // Load the table information.
        this.tables = statement.getTables().stream()
                .map(functionWrapper(table -> new PlanTableNode(connectionInfo, table)))
                .collect(Collectors.toList());

        parseColumns(statement);
        groupBy = parseGroupBy(statement);
        parseOrderBy(statement);

        if (this.columns.isEmpty()) {
            throw new ParadoxSyntaxErrorException(SyntaxError.EMPTY_COLUMN_LIST);
        }
    }

    /**
     * Parses the table columns.
     *
     * @param statement the SELECT statement.
     * @throws SQLException in case of parse errors.
     */
    private void parseColumns(final SelectNode statement) throws SQLException {
        for (final SQLNode field : statement.getFields()) {
            if (field instanceof AsteriskNode) {
                if (this.tables.isEmpty()) {
                    throw new ParadoxSyntaxErrorException(SyntaxError.ASTERISK_WITHOUT_TABLE,
                            field.getPosition());
                }
                parseAsterisk((AsteriskNode) field);
            } else {
                addColumn((FieldNode) field);
            }
        }
    }

    /**
     * Parses the group by fields.
     *
     * @param statement the SELECT statement.
     * @return <code>true</code> if this statement uses a group by.
     * @throws SQLException in case of parse errors.
     */
    private boolean parseGroupBy(final SelectNode statement) throws SQLException {

        // Create columns to use in SELECT statement.
        for (final FieldNode field : statement.getGroups()) {
            if (field instanceof ParameterNode) {
                throw new ParadoxNotSupportedException(ParadoxNotSupportedException.Error.OPERATION_NOT_SUPPORTED,
                        field.getPosition());
            } else if (field instanceof ValueNode) {
                addGroupColumn(new Column((ValueNode) field));
            } else {
                addGroupColumn(field);
            }
        }

        final boolean grouping = this.columns.stream()
                .map(Column::getFunction)
                .filter(Objects::nonNull)
                .anyMatch(FunctionNode::isGrouping) || !groupByFields.isEmpty();

        if (grouping) {
            // Validate statically the group by clause.
            final Set<Column> groupColumnsToCheck = new HashSet<>(groupByFields);
            final List<Column> fields = this.columns.stream()
                    .filter(c -> c.getFunction() == null || !c.getFunction().isGrouping())
                    .filter(c -> !groupColumnsToCheck.remove(c))
                    .collect(Collectors.toList());

            if (!fields.isEmpty()) {
                throw new ParadoxSyntaxErrorException(SyntaxError.NOT_GROUP_BY);
            }
        }

        return grouping;
    }

    /**
     * Parses the order by fields.
     *
     * @param statement the SELECT statement.
     * @throws SQLException in case of parse errors.
     */
    private void parseOrderBy(final SelectNode statement) throws SQLException {
        for (int i = 0; i < statement.getOrder().size(); i++) {
            final FieldNode field = statement.getOrder().get(i);
            final OrderType type = statement.getOrderTypes().get(i);
            if (field instanceof ValueNode) {
                int index = ValuesConverter.getInteger(field.getName());
                if (index > this.columns.size()) {
                    throw new ParadoxException(ParadoxException.Error.INVALID_COLUMN_INDEX, index);
                }

                addOrderColumn(this.columns.get(index - 1), type);
            } else {
                addOrderColumn(field, type);
            }
        }

        // This is a group by expression?
        if (groupBy) {
            final List<Column> columnsFound = this.columns.stream()
                    .filter(c -> !c.isHidden()).collect(Collectors.toList());
            if (!columnsFound.containsAll(orderByFields)) {
                throw new ParadoxSyntaxErrorException(SyntaxError.ORDER_BY_NOT_IN_GROUP_BY);
            }
        }
    }

    /**
     * Parses the asterisk fields in SELECT.
     *
     * @param field the asterisk field.
     * @throws SQLException in case of parse errors.
     */
    private void parseAsterisk(final AsteriskNode field) throws SQLException {
        if (field.getTableName() != null) {
            final List<ParadoxTable> tablesFound = this.tables.stream()
                    .filter(t -> t.isThis(field.getTableName()))
                    .map(PlanTableNode::getTable).collect(Collectors.toList());

            if (tablesFound.isEmpty()) {
                throw new ParadoxDataException(ParadoxDataException.Error.TABLE_NOT_FOUND, field.getPosition(),
                        field.getTableName());
            } else if (tablesFound.size() > 1) {
                throw new ParadoxException(ParadoxException.Error.TABLE_AMBIGUOUS_DEFINED, field.getPosition(),
                        field.getTableName());
            }

            addColumnFromTable(tablesFound.get(0));
        } else {
            addColumnFromTables();
        }
    }

    private static AbstractConditionalNode reduce(final AbstractConditionalNode node) {
        AbstractConditionalNode ret = node;

        // It is an AND and OR node?
        if (node instanceof AbstractJoinNode) {
            final List<SQLNode> children = node.getChildren();

            // Reduce all children.
            for (int loop = 0; loop < children.size(); loop++) {
                children.set(loop, reduce((AbstractConditionalNode) children.get(loop)));
            }

            // Reduce only AND and OR nodes.
            while (ret instanceof AbstractJoinNode && ret.getChildren().size() <= 1) {
                if (ret.getChildren().isEmpty()) {
                    ret = null;
                } else {
                    ret = (AbstractConditionalNode) ret.getChildren().get(0);
                }
            }
        }

        return ret;
    }

    private static void addAndClause(final PlanTableNode table, SQLNode clause) {
        if (table.getConditionalJoin() instanceof ANDNode) {
            // Exists and it is an AND node.
            table.getConditionalJoin().addChild(clause);
        } else if (table.getConditionalJoin() != null) {
            // Exists, but any other type.
            final ANDNode andNode = new ANDNode(table.getConditionalJoin(), null);
            andNode.addChild(clause);
            table.setConditionalJoin(andNode);
        } else {
            // There is no conditionals in this table.
            table.setConditionalJoin((AbstractConditionalNode) clause);
        }
    }

    private static void getConditionalFields(final PlanTableNode table, final Set<Column> columnsToLoad,
                                             final AbstractConditionalNode condition) {
        if (condition != null) {
            final Set<FieldNode> fields = condition.getClauseFields();
            fields.forEach((FieldNode node) -> {
                if (table.isThis(node.getTableName())) {
                    Arrays.stream(table.getTable().getFields())
                            .filter(f -> f.getName().equalsIgnoreCase(node.getName())).map(Column::new)
                            .forEach(columnsToLoad::add);
                }
            });
        }
    }

    @Override
    public void optimize() {
        if (optimizeConditions(condition)) {
            condition = null;
        }

        // Reduce default conditions.
        condition = reduce(condition);

        // Reduce table conditions.
        for (final PlanTableNode table : this.tables) {
            table.setConditionalJoin(reduce(table.getConditionalJoin()));
        }

        // Sets the column indexes.
        for (int i = 0; i < this.columns.size(); i++) {
            this.columns.get(i).setIndex(i);
        }

        processGroupBy();
    }

    private void processGroupBy() {
        createGroupByColumns();

        // Columns with a grouping function.
        groupFunctionColumns = this.columns.stream()
                .map(Column::getFunction)
                .filter(Objects::nonNull)
                .filter(FunctionNode::isGrouping)
                .filter(f -> !f.isSecondPass())
                .mapToInt(FunctionNode::getIndex)
                .toArray();

        // Key columns to do the grouping.
        final HashSet<Column> columnsToCheck = new HashSet<>(groupByFields);
        groupColumns = this.columns.stream()
                .filter(c -> c.getFunction() == null || !c.getFunction().isGrouping())
                .filter(c -> !c.isHidden() || columnsToCheck.remove(c))
                .mapToInt(Column::getIndex)
                .toArray();
    }

    private void createGroupByColumns() {
        final List<Column> columnList = this.columns.stream()
                .map(SelectPlan::getGroupingFunctions).flatMap(Collection::stream)
                .map(Column::new)
                .collect(Collectors.toList());

        for (final Column column : columnList) {
            final int index = this.columns.indexOf(column);
            if (index < 0) {
                column.setHidden(true);
                column.getFunction().setIndex(this.columns.size());
                this.columns.add(column);
            } else {
                column.getFunction().setIndex(index);
            }
        }
    }

    private static List<FunctionNode> getGroupingFunctions(final Column column) {
        final FunctionNode function = column.getFunction();
        if (function == null) {
            return Collections.emptyList();
        }

        return function.getGroupingNodes();
    }

    private int getTableIndex(final ParadoxTable table) {
        int index = -1;
        for (int i = 0; i < this.tables.size(); i++) {
            if (this.tables.get(i).getTable().equals(table)) {
                index = i;
                break;
            }
        }

        return index;
    }

    private PlanTableNode getPlanTable(final ParadoxTable table) {
        return tables.stream().filter(t -> table.equals(t.getTable())).findFirst().orElse(null);
    }

    /**
     * Associate all columns from a table.
     *
     * @param table the table to scan.
     */
    private void addColumnFromTable(final ParadoxDataFile table) {
        for (final ParadoxField field : table.getFields()) {
            this.columns.add(new Column(field));
        }
    }

    /**
     * Associate all columns from a list of tables.
     */
    private void addColumnFromTables() {
        for (final PlanTableNode table : this.tables) {
            addColumnFromTable(table.getTable());
        }
    }

    /**
     * Add column from select list.
     *
     * @param node SQL node with column attributes.
     * @throws SQLException search column exception.
     */
    private void addColumn(final FieldNode node) throws SQLException {
        if (node instanceof ValueNode) {
            this.columns.add(new Column((ValueNode) node));
        } else if (node instanceof ParameterNode) {
            this.columns.add(new Column((ParameterNode) node));
        } else if (node instanceof FunctionNode) {
            final List<Column> columnsToProcess = getParadoxFields(node);

            // The fist column is always the function column.
            final Column column = columnsToProcess.get(0);
            column.setName(node.getAlias());
            columns.add(column);
            this.columnsFromFunctions.addAll(columnsToProcess);
        } else {
            final List<Column> columnsToProcess = getParadoxFields(node);
            columnsToProcess.forEach((Column column) -> {
                column.setName(node.getAlias());
                this.columns.add(column);
            });
        }
    }

    private List<Column> getParadoxFields(final FieldNode node) throws ParadoxException {
        final List<Column> ret = new ArrayList<>();

        if (node instanceof FunctionNode) {
            final FunctionNode functionNode = (FunctionNode) node;

            // Create the column for the function.
            ret.add(new Column(functionNode));

            // Parses function fields in function parameters.
            for (final FieldNode field : functionNode.getClauseFields()) {
                ret.addAll(getParadoxFields(field));
            }
        } else if (!(node instanceof ValueNode) && !(node instanceof ParameterNode)
                && !(node instanceof AsteriskNode)) {
            for (final PlanTableNode table : this.tables) {
                if (node.getTableName() == null || table.isThis(node.getTableName())) {
                    node.setTable(table.getTable());
                    ret.addAll(Arrays.stream(table.getTable().getFields())
                            .filter(f -> f.getName().equalsIgnoreCase(node.getName()))
                            .map(Column::new)
                            .collect(Collectors.toList()));
                }
            }

            if (ret.isEmpty()) {
                throw new ParadoxException(ParadoxException.Error.INVALID_COLUMN, node.getPosition(), node.toString());
            } else if (ret.size() > 1) {
                throw new ParadoxException(ParadoxException.Error.COLUMN_AMBIGUOUS_DEFINED, node.getPosition(),
                        node.toString());
            }
        }

        return ret;
    }

    /**
     * Add a order by column to this plan.
     *
     * @param node the node to convert to a column.
     * @param type the order by field type.
     * @throws SQLException in case of failures.
     */
    private void addOrderColumn(final FieldNode node, final OrderType type) throws SQLException {
        getParadoxFields(node).forEach(column -> addOrderColumn(column, type));
    }

    /**
     * Add a order by column to this plan.
     *
     * @param column the column to add.
     * @param type   the order by field type.
     */
    private void addOrderColumn(final Column column, final OrderType type) {
        this.orderByFields.add(column);
        this.orderTypes.add(type);

        if (!this.columns.contains(column)) {
            // If not in SELECT statement, add as a hidden column in ResultSet.
            column.setHidden(true);
            this.columns.add(column);
        }
    }

    /**
     * Add a group by column to this plan.
     *
     * @param node the node to convert to a column.
     * @throws SQLException in case of failures.
     */
    private void addGroupColumn(final FieldNode node) throws SQLException {
        getParadoxFields(node).forEach(this::addGroupColumn);
    }

    /**
     * Add a group by column to this plan.
     *
     * @param column the column to add.
     */
    private void addGroupColumn(final Column column) {
        groupByFields.add(column);

        if (!this.columns.contains(column)) {
            // If not in SELECT statement, add as a hidden column in ResultSet.
            column.setHidden(true);
            this.columns.add(column);
        }
    }

    /**
     * Process the node and change it to it's table (if it is possible).
     *
     * @param node the node to process.
     * @return <code>true</code> if the node is processed and needed to be removed.
     */
    @SuppressWarnings({"java:S3776", "java:S1541"})
    private boolean optimizeConditions(final SQLNode node) {
        boolean ret = false;
        if (node instanceof ANDNode) {
            ANDNode andNode = (ANDNode) node;
            andNode.getChildren().removeIf(this::optimizeConditions);
            ret = node.getClauseFields().isEmpty();
        } else if (node != null && !(node instanceof ORNode)) {
            // Don't process OR nodes.
            final List<ParadoxField> conditionalFields = new ArrayList<>();

            final Set<FieldNode> fields = node.getClauseFields();
            fields.forEach((FieldNode fn) -> {
                for (final PlanTableNode table : this.tables) {
                    if (table.isThis(fn.getTableName())) {
                        conditionalFields.addAll(Arrays.stream(table.getTable().getFields())
                                .filter(f -> f.getName().equalsIgnoreCase(fn.getName()))
                                .collect(Collectors.toSet()));
                    }
                }
            });

            if (conditionalFields.size() == 1) {
                // FIELD = VALUE

                final ParadoxTable paradoxTable = conditionalFields.get(0).getTable();
                final PlanTableNode planTableNode = getPlanTable(paradoxTable);

                if (planTableNode != null && (planTableNode.getJoinType() == JoinType.CROSS
                        || planTableNode.getJoinType() == JoinType.INNER)) {
                    // Do not change OUTER joins.
                    addAndClause(planTableNode, node);
                    ret = true;
                }
            } else if (conditionalFields.size() > 1) {
                // FIELD = FIELD
                final ParadoxTable paradoxTable1 = conditionalFields.get(0).getTable();
                final ParadoxTable paradoxTable2 = conditionalFields.get(1).getTable();

                final int index1 = getTableIndex(paradoxTable1);
                final int index2 = getTableIndex(paradoxTable2);

                // Both tables exists?
                if (index1 != -1 && index2 != -1) {
                    // Use the last table to
                    int lastIndex = Math.max(index1, index2);

                    addAndClause(this.tables.get(lastIndex), node);
                    ret = true;
                }
            }
        }

        // Unprocessed.
        return ret;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    @SuppressWarnings({"java:S3776", "java:S1541"})
    public List<Object[]> execute(final ConnectionInfo connectionInfo, final int maxRows, final Object[] parameters,
                                  final ParadoxType[] parameterTypes) throws SQLException {

        // Can't do anything without fields defined.
        if (this.columns.isEmpty()) {
            return Collections.emptyList();
        }

        // Reset the cancelled state.
        cancelled = false;
        joiner.reset();

        final List<Column> columnsLoaded = new ArrayList<>();
        final List<Object[]> rawData = new ArrayList<>(0xFF);

        for (int tableIndex = 0; tableIndex < this.tables.size(); tableIndex++) {
            PlanTableNode table = this.tables.get(tableIndex);
            ensureNotCancelled();

            // Columns in SELECT clause.
            final Set<Column> tableColumns = this.columns.stream()
                    .filter(c -> c.isThis(table.getTable()))
                    .collect(Collectors.toSet());

            // Columns in SELECT functions.
            tableColumns.addAll(this.columnsFromFunctions.stream()
                    .filter(c -> c.isThis(table.getTable()))
                    .collect(Collectors.toSet()));

            // Columns in GROUP BY clause.
            tableColumns.addAll(this.groupByFields.stream()
                    .filter(c -> c.isThis(table.getTable()))
                    .collect(Collectors.toSet()));

            // Columns in ORDER BY clause.
            tableColumns.addAll(this.orderByFields.stream()
                    .filter(c -> c.isThis(table.getTable()))
                    .collect(Collectors.toSet()));

            // Fields from WHERE clause.
            getConditionalFields(table, tableColumns, this.condition);

            // Get fields from other tables join.
            for (final PlanTableNode tableToField : this.tables) {
                getConditionalFields(table, tableColumns, tableToField.getConditionalJoin());
            }

            // If there is a column to load.
            if (tableColumns.isEmpty()) {
                // Force the table loading (used in joins).
                tableColumns.add(new Column(table.getTable().getFields()[0]));
            }

            columnsLoaded.addAll(tableColumns);

            final List<Object[]> tableData = TableData.loadData(table.getTable(),
                    tableColumns.stream().map(Column::getField).toArray(ParadoxField[]::new));
            if (table.getConditionalJoin() != null) {
                table.getConditionalJoin().setFieldIndexes(columnsLoaded, this.tables);
            }

            // First table?
            if (tableIndex == 0) {
                if (table.getConditionalJoin() != null) {
                    rawData.addAll(tableData.stream()
                            .filter(predicateWrapper(o -> ensureNotCancelled()))
                            .filter(predicateWrapper(tableRow -> table.getConditionalJoin()
                                    .evaluate(connectionInfo, tableRow, parameters, parameterTypes, columnsLoaded)))
                            .collect(Collectors.toList()));
                } else {
                    // No conditions to process. Just use it.
                    rawData.addAll(tableData);
                }
            } else {
                final List<Object[]> ret = joiner.processJoinByType(connectionInfo, columnsLoaded, rawData, table,
                        tableData, parameters, parameterTypes);

                rawData.clear();
                rawData.addAll(ret);
            }
        }

        // Stop here if there is no value to process.
        if (tables.isEmpty()) {
            final Object[] row = new Object[this.columns.size()];
            for (int i = 0; i < row.length; i++) {
                // A list of fixed value.
                row[i] = this.columns.get(i).getValue();
            }

            rawData.add(row);
        } else if (rawData.isEmpty()) {
            // No result to process, just return.
            return Collections.emptyList();
        }

        processIndexes(columnsLoaded);
        processFunctionIndexes(columnsLoaded);
        processSelectParameters(this.columns, parameterTypes);

        // Find column indexes.
        final int[] mapColumns = mapColumnIndexes(columnsLoaded);

        filter(connectionInfo, rawData, mapColumns, maxRows, parameters, parameterTypes, columnsLoaded);

        return this.values;
    }

    private static void processSelectParameters(final List<Column> columns, final ParadoxType[] parameterTypes) {
        for (final Column column : columns) {
            final ParameterNode parameterNode = column.getParameter();
            if (parameterNode != null) {
                column.setType(parameterTypes[parameterNode.getParameterIndex()]);
            }
        }
    }

    private Comparator<Object[]> processOrderBy() {
        if (orderByFields.isEmpty()) {
            // Nothing to do here, there are no order by fields.
            return null;
        }

        final int[] mapColumns = new int[this.orderByFields.size()];
        Arrays.fill(mapColumns, -1);
        for (int i = 0; i < this.orderByFields.size(); i++) {
            final Column column = this.orderByFields.get(i);
            mapColumns[i] = column.getIndex();
            for (int loop = 0; loop < this.columns.size(); loop++) {
                if (this.columns.get(loop).equals(column)) {
                    mapColumns[i] = loop;
                    break;
                }
            }
        }

        // Build the comparator list.
        Comparator<Object[]> comparator = null;
        for (int i = 0; i < mapColumns.length; i++) {
            final int index = mapColumns[i];
            final OrderByComparator orderByComparator = new OrderByComparator(index, orderTypes.get(i));
            if (comparator == null) {
                comparator = orderByComparator;
            } else {
                comparator = comparator.thenComparing(orderByComparator);
            }
        }

        return comparator;
    }

    private void processIndexes(final List<Column> columns) throws SQLException {
        // Set conditional indexes.
        if (this.condition != null) {
            this.condition.setFieldIndexes(columns, tables);
        }

        // Set table join indexes.
        for (final PlanTableNode table : this.tables) {
            if (table.getConditionalJoin() != null) {
                table.getConditionalJoin().setFieldIndexes(columns, tables);
            }
        }
    }

    private void processFunctionIndexes(final List<Column> columnsLoaded) throws SQLException {
        for (final Column column : this.columns) {
            if (column.getFunction() != null) {
                FieldValueUtils.setFunctionIndexes(column.getFunction(), columnsLoaded, this.tables);
            }
        }
    }

    private int[] mapColumnIndexes(final List<Column> loadedColumns) {
        final int[] mapColumns = new int[this.columns.size()];
        Arrays.fill(mapColumns, -1);
        for (int i = 0; i < this.columns.size(); i++) {
            final Column column = this.columns.get(i);
            for (int loop = 0; loop < loadedColumns.size(); loop++) {
                if (loadedColumns.get(loop).getField().equals(column.getField())) {
                    mapColumns[i] = loop;
                    break;
                }
            }
        }

        return mapColumns;
    }

    private Object[] mapRow(final ConnectionInfo connectionInfo, final Object[] tableRow, final int[] mapColumns,
                            final Object[] parameters, final ParadoxType[] parameterTypes,
                            final List<Column> columnsLoaded) throws SQLException {
        final Object[] finalRow = new Object[mapColumns.length];
        for (int i = 0; i < mapColumns.length; i++) {
            ensureNotCancelled();

            int index = mapColumns[i];
            if (index != -1) {
                // A field mapped value.
                finalRow[i] = tableRow[index];
            } else {
                final ParameterNode parameterNode = this.columns.get(i).getParameter();
                final FunctionNode functionNode = this.columns.get(i).getFunction();
                if (parameterNode != null) {
                    // A parameter value.
                    finalRow[i] = parameters[parameterNode.getParameterIndex()];
                } else if (functionNode == null) {
                    // A fixed value.
                    finalRow[i] = this.columns.get(i).getValue();
                } else if (!this.columns.get(i).isSecondPass()) {
                    // A function processed value.
                    finalRow[i] = functionNode.execute(connectionInfo, tableRow, parameters, parameterTypes,
                            columnsLoaded);
                    // The function may change the result type in execution based on parameters values.
                    this.columns.get(i).setType(functionNode.getType());
                }
            }
        }

        return finalRow;
    }

    private void filter(final ConnectionInfo connectionInfo, final List<Object[]> rowValues, final int[] mapColumns,
                        final int maxRows, final Object[] parameters, final ParadoxType[] parameterTypes,
                        final List<Column> columnsLoaded) {

        Stream<Object[]> stream = rowValues.parallelStream()
                .filter(predicateWrapper(c -> ensureNotCancelled()));

        if (condition != null) {
            stream = stream.filter(predicateWrapper((Object[] tableRow) ->
                    condition.evaluate(connectionInfo, tableRow, parameters, parameterTypes, columnsLoaded)
            ));
        }

        stream = stream.map(functionWrapper((Object[] tableRow) ->
                mapRow(connectionInfo, tableRow, mapColumns, parameters, parameterTypes, columnsLoaded)
        ));

        if (groupBy) {
            // Is not possible to group in parallel.
            stream = stream.sequential()
                    .filter(FunctionalUtils.groupingByKeys(groupFunctionColumns, groupColumns))
                    .collect(Collectors.toList())
                    .stream()
                    .filter(predicateWrapper(c -> ensureNotCancelled()))
                    .map(functionWrapper(FunctionalUtils.removeGrouping(groupFunctionColumns, connectionInfo,
                            parameters, parameterTypes, this.columns)));
        }

        if (!orderByFields.isEmpty()) {
            stream = stream.sorted(Objects.requireNonNull(processOrderBy()));
        }

        if (distinct) {
            // Is not possible to group in parallel fashion (distinct).
            stream = stream.sequential().filter(FunctionalUtils.distinctByKey());
        }

        if (maxRows != 0) {
            stream = stream.limit(maxRows);
        }

        this.values = stream.collect(Collectors.toList());
    }

    /**
     * Gets the columns in SELECT statement.
     *
     * @return the columns in SELECT statement.
     */
    public List<Column> getColumns() {
        return this.columns;
    }

    /**
     * Gets the statements WHERE conditions.
     *
     * @return the statements WHERE conditions.
     */
    public AbstractConditionalNode getCondition() {
        return condition;
    }

    /**
     * Cancel this statement execution.
     */
    @Override
    public void cancel() {
        cancelled = true;
        joiner.cancel();
    }

    /**
     * Check if this execution was cancelled.
     *
     * @return <code>true</code> if this statement was not cancelled.
     * @throws SQLException if this execution was cancelled.
     */
    private boolean ensureNotCancelled() throws SQLException {
        if (cancelled) {
            throw new ParadoxException(ParadoxException.Error.OPERATION_CANCELLED);
        }

        return true;
    }

    /**
     * Gets the group by fields.
     *
     * @return the group by fields.
     */
    public List<Column> getGroupByFields() {
        return groupByFields;
    }

    /**
     * Gets the order by fields.
     *
     * @return the order by fields.
     */
    public List<Column> getOrderByFields() {
        return orderByFields;
    }

    /**
     * Gets the tables.
     *
     * @return the tables.
     */
    public List<PlanTableNode> getTables() {
        return tables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SelectPlan that = (SelectPlan) o;
        return distinct == that.distinct && Objects.equals(columns, that.columns) && Objects.equals(tables, that.tables)
                && Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, tables, distinct, condition);
    }
}
