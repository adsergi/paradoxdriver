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
package com.googlecode.paradox.data;

import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.data.table.value.FieldValue;
import com.googlecode.paradox.integration.MainTest;
import com.googlecode.paradox.metadata.ParadoxField;
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.utils.TestUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link TableData}.
 *
 * @author Leonardo Alves da Costa
 * @version 1.1
 * @since 1.0
 */
public class TableDataTest {

    /**
     * The database connection.
     */
    private ParadoxConnection conn;

    /**
     * Register the driver.
     *
     * @throws ClassNotFoundException in case of connection errors.
     */
    @BeforeClass
    public static void initClass() throws ClassNotFoundException {
        Class.forName(Driver.class.getName());
    }

    /**
     * Used to close the test connection.
     *
     * @throws Exception in case closing of errors.
     */
    @After
    public void closeConnection() throws Exception {
        if (this.conn != null) {
            this.conn.close();
        }
    }

    /**
     * Connect to test database.
     *
     * @throws Exception in case of connection errors.
     */
    @Before
    public void connect() throws Exception {
        this.conn = (ParadoxConnection) DriverManager.getConnection(MainTest.CONNECTION_STRING + "db");
    }

    /**
     * Test for invalid table
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testInvalidTable() throws SQLException {
        Assert.assertEquals("Failed in count invalid tables.", 0,
                TableData.listTables(this.conn.getCurrentSchema(), "not found.db", this.conn).size());
    }

    /**
     * Test for table area codes.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadAreaCodes() throws SQLException {
        final List<ParadoxTable> tables = TableData.listTables(this.conn.getCurrentSchema(), "areacodes.db", this.conn);
        Assert.assertNotNull("List tables is null", tables);
        Assert.assertTrue("List tables is empty", tables.size() > 0);
        final ParadoxTable table = tables.get(0);
        final List<List<FieldValue>> data = TableData.loadData(table, table.getFields());
        Assert.assertEquals("Error in load table.", table.getRowCount(), data.size());
    }

    /**
     * Test for contact table.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadContacts() throws SQLException {
        final ParadoxTable table = TableData.listTables(this.conn.getCurrentSchema(), "contacts.db", this.conn).get(0);
        final ArrayList<ParadoxField> fields = new ArrayList<>();
        fields.add(table.getFields().get(0));
        Assert.assertNotNull("Error loading table list.", TableData.loadData(table, fields));
    }

    /**
     * Test for customer table.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadCustomer() throws SQLException {
        final ParadoxTable table = TableData.listTables(this.conn.getCurrentSchema(), "customer.db", this.conn).get(0);
        final ArrayList<ParadoxField> fields = new ArrayList<>();
        fields.add(table.getFields().get(0));
        Assert.assertNotNull("Error loading table list.", TableData.loadData(table, fields));
    }

    /**
     * Test for Hercules table.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadHercules() throws SQLException {
        final ParadoxTable table = TableData.listTables(this.conn.getCurrentSchema(), "hercules.db", this.conn).get(0);
        Assert.assertNotNull("Error loading table list.", TableData.loadData(table, table.getFields()));
    }

    /**
     * Test for orders table.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadOrders() throws SQLException {
        final ParadoxTable table = TableData.listTables(this.conn.getCurrentSchema(), "orders.db", this.conn).get(0);
        final ArrayList<ParadoxField> fields = new ArrayList<>();
        fields.add(table.getFields().get(0));
        Assert.assertNotNull("Error loading table list.", TableData.loadData(table, fields));
    }

    /**
     * Test for server table.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLoadServer() throws SQLException {
        final ParadoxTable table = TableData.listTables(this.conn.getCurrentSchema(), "server.db", this.conn).get(0);
        final ArrayList<ParadoxField> fields = new ArrayList<>();
        fields.add(table.getFields().get(0));
        Assert.assertNotNull("Error loading table list.", TableData.loadData(table, fields));
    }

    /**
     * Test for class sanity.
     *
     * @throws NoSuchMethodException     in case of errors.
     * @throws InstantiationException    in case of errors.
     * @throws IllegalAccessException    in case of errors.
     * @throws InvocationTargetException in case of errors.
     */
    @Test
    public void testSanity()
    throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Assert.assertTrue("Utility class in wrong format.", TestUtil.assertUtilityClassWellDefined(TableData.class));
    }
}
