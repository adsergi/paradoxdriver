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

import com.googlecode.paradox.Driver;
import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.data.TableData;
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.parser.nodes.TableNode;
import com.googlecode.paradox.planner.nodes.PlanTableNode;
import org.junit.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Unit test for {@link SelectPlan} class.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.3
 */
public class SelectPlanTest {

    /**
     * The connection string used in this tests.
     */
    private static final String CONNECTION_STRING = "jdbc:paradox:target/test-classes/";
    private static final String AREACODES = "areacodes";

    /**
     * The database connection.
     */
    private ParadoxConnection conn;

    /**
     * Register the database driver.
     *
     * @throws ClassNotFoundException in case of failures.
     */
    @BeforeClass
    public static void initClass() throws ClassNotFoundException {
        Class.forName(Driver.class.getName());
    }

    /**
     * Close the test connection.
     *
     * @throws SQLException in case of failures.
     */
    @After
    public void closeConnection() throws SQLException {
        if (this.conn != null) {
            this.conn.close();
        }
    }

    /**
     * Connect to the test database.
     *
     * @throws SQLException in case of failures.
     */
    @Before
    public void connect() throws SQLException {
        this.conn = (ParadoxConnection) DriverManager.getConnection(SelectPlanTest.CONNECTION_STRING + "db");
    }

    /**
     * Test for ambiguous column table alias.
     *
     * @throws SQLException if has errors.
     */
    @Test(expected = SQLException.class)
    public void testAmbiguousColumn() throws SQLException {
        final SelectPlan plan = new SelectPlan();

        TableNode table = new TableNode(conn, null, AREACODES, "test");

        final List<ParadoxTable> tables = TableData.listTables(this.conn.getCurrentSchema(), AREACODES, this.conn);
        PlanTableNode tableNode = new PlanTableNode();
        tableNode.setTable(conn.getSchema(), table, tables);
        plan.addTable(tableNode);

        tableNode = new PlanTableNode();
        table.setAlias("test2");
        tableNode.setTable(conn.getSchema(), table, tables);
        plan.addTable(tableNode);

        plan.addColumn("ac");
        Assert.assertEquals("Invalid column size.", 1, plan.getColumns().size());
    }

    /**
     * Test for column value with table alias.
     *
     * @throws SQLException if has errors.
     */
    @Test
    public void testColumnWithTableAlias() throws SQLException {
        final SelectPlan plan = new SelectPlan();

        TableNode table = new TableNode(conn, null, AREACODES, "test");

        final List<ParadoxTable> tables = TableData.listTables(this.conn.getCurrentSchema(), AREACODES, this.conn);
        PlanTableNode tableNode = new PlanTableNode();
        tableNode.setTable(conn.getSchema(), table, tables);
        plan.addTable(tableNode);

        plan.addColumn("test.ac");
        Assert.assertEquals("Invalid column size.", 1, plan.getColumns().size());
    }

    /**
     * Test for invalid column value.
     *
     * @throws SQLException if there are no errors.
     */
    @Test(expected = SQLException.class)
    public void testInvalidColumn() throws SQLException {
        final SelectPlan plan = new SelectPlan();
        plan.addColumn("invalid");
    }

    /**
     * Test for invalid table alias.
     *
     * @throws SQLException if has errors.
     */
    @Test(expected = SQLException.class)
    public void testInvalidTableAlias() throws SQLException {
        final SelectPlan plan = new SelectPlan();

        TableNode table = new TableNode(conn, null, AREACODES, "test");

        final List<ParadoxTable> tables = TableData.listTables(this.conn.getCurrentSchema(), AREACODES, this.conn);
        PlanTableNode tableNode = new PlanTableNode();
        tableNode.setTable(conn.getSchema(), table, tables);
        plan.addTable(tableNode);

        plan.addColumn("test2.ac");
    }
}
