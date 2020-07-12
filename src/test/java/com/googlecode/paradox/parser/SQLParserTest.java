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
package com.googlecode.paradox.parser;

import com.googlecode.paradox.Driver;
import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.exceptions.ParadoxSyntaxErrorException;
import com.googlecode.paradox.parser.nodes.*;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.planner.nodes.ParameterNode;
import com.googlecode.paradox.planner.nodes.ValueNode;
import com.googlecode.paradox.planner.nodes.comparable.*;
import com.googlecode.paradox.planner.nodes.join.ANDNode;
import com.googlecode.paradox.planner.nodes.join.ORNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Unit test for {@link SQLParser}.
 *
 * @version 1.5
 * @since 1.0
 */
@SuppressWarnings({"java:S109", "java:S1192"})
public class SQLParserTest {

    /**
     * The connection string used in this tests.
     */
    public static final String CONNECTION_STRING = "jdbc:paradox:target/test-classes/";

    /**
     * The database connection.
     */
    private static ParadoxConnection conn;

    /**
     * Register the database driver.
     *
     * @throws SQLException in case of failures.
     */
    @BeforeClass
    @SuppressWarnings("java:S2115")
    public static void setUp() throws SQLException {
        new Driver();
        conn = (ParadoxConnection) DriverManager.getConnection(CONNECTION_STRING + "db");
    }

    /**
     * Close the database connection.
     *
     * @throws SQLException in case of failures.
     */
    @AfterClass
    public static void tearDown() throws SQLException {
        conn.close();
    }

    /**
     * Test for is null expressions.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testIsNull() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT A FROM db.B WHERE A is NULL");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertTrue("Invalid condition value.", select.getCondition() instanceof IsNullNode);
        final IsNullNode node = (IsNullNode) select.getCondition();
        Assert.assertEquals("Invalid field name.", "A", node.getField().getName());
        Assert.assertNull("Invalid field value.", node.getLast());
    }

    /**
     * Test like expressions.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testLike() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select ac.AreasCovered from geog.tblAC ac " +
                " where ac.AreasCovered like 'Hackensack%'");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertTrue("Invalid condition value.", select.getCondition() instanceof LikeNode);
        final LikeNode node = (LikeNode) select.getCondition();
        Assert.assertEquals("Invalid field name.", "AreasCovered", node.getField().getName());
        Assert.assertEquals("Invalid field value.", "Hackensack%", node.getLast().getName());
    }

    /**
     * Test ilike expressions.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testILike() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select ac.AreasCovered from geog.tblAC ac " +
                " where ac.AreasCovered ilike 'Hackensack%'");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertTrue("Invalid condition value.", select.getCondition() instanceof ILikeNode);
        final ILikeNode node = (ILikeNode) select.getCondition();
        Assert.assertEquals("Invalid field name.", "AreasCovered", node.getField().getName());
        Assert.assertEquals("Invalid field value.", "Hackensack%", node.getLast().getName());
    }

    /**
     * Test for is not null expressions.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testIsNotNull() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT A FROM db.B WHERE A is not NULL");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertTrue("Invalid condition value.", select.getCondition() instanceof IsNotNullNode);
        final IsNotNullNode node = (IsNotNullNode) select.getCondition();
        Assert.assertEquals("Invalid field name.", "A", node.getField().getName());
        Assert.assertNull("Invalid field value.", node.getLast());
    }

    /**
     * Test for not as a value.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testNullAsValue() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT A FROM db.B WHERE A = NULL");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertTrue("Invalid condition value.", select.getCondition() instanceof EqualsNode);
        final EqualsNode node = (EqualsNode) select.getCondition();
        Assert.assertEquals("Invalid field name.", "A", node.getField().getName());
        Assert.assertTrue("Invalid field value.", node.getLast() instanceof ValueNode);
    }

    /**
     * Test select with alias in fields.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSelectWithAlias() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT t.* FROM table t");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        final SQLNode node = select.getFields().get(0);

        Assert.assertTrue("Invalid node type.", node instanceof AsteriskNode);
        final AsteriskNode asteriskNode = (AsteriskNode) node;

        Assert.assertEquals("Invalid value.", "t", asteriskNode.getTableName());
    }

    /**
     * Test for schema name.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSchemaName() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT t.* FROM db.table t");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        final SQLNode node = select.getFields().get(0);

        Assert.assertTrue("Invalid node type.", node instanceof AsteriskNode);
        final AsteriskNode asteriskNode = (AsteriskNode) node;

        Assert.assertEquals("Invalid value.", "t", asteriskNode.getTableName());
    }

    /**
     * Test for column values.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testColumnValue() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT 'test', 123 as number, null FROM client");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 3, select.getFields().size());

        Assert.assertTrue("Invalid node type.", select.getFields().get(0) instanceof ValueNode);
        Assert.assertEquals("Invalid node name.", "test", select.getFields().get(0).getName());

        Assert.assertTrue("Invalid node type.", select.getFields().get(1) instanceof ValueNode);
        Assert.assertEquals("Invalid node name.", "123", select.getFields().get(1).getName());
        Assert.assertEquals("Invalid node alias.", "number", select.getFields().get(1).getAlias());

        Assert.assertTrue("Invalid node type.", select.getFields().get(2) instanceof ValueNode);
        Assert.assertNull("Invalid node name.", select.getFields().get(2).getName());
        Assert.assertEquals("Invalid node alias.", "null", select.getFields().get(2).getAlias());

        Assert.assertEquals("Invalid node size.", 1, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
    }

    /**
     * Test for join token.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testJoin() throws SQLException {
        final SQLParser parser = new SQLParser(conn,
                "SELECT * FROM client c inner join test t on test_id = id and a <> b left join table on a = b");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 3, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
    }

    /**
     * Test for full join token.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testFullJoin() throws SQLException {
        final SQLParser parser = new SQLParser(conn,
                "SELECT * FROM client c full join test t on test_id = id ");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 2, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
        Assert.assertEquals("Invalid node name.", "test", select.getTables().get(1).getName());
        Assert.assertTrue("Invalid node type", select.getTables().get(1) instanceof JoinNode);
        Assert.assertEquals("Invalid node name.", JoinType.FULL, ((JoinNode) select.getTables().get(1)).getJoinType());
    }

    /**
     * Test for SELECT token.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSelect() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT * FROM client");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 1, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
    }

    /**
     * Test for tables.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testTable() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT * FROM \"client.db\"");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 1, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
    }

    /**
     * Test a SELECT with two tables.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testTwoTable() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select a.CODE as cod, state.NAME name FROM client, state");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 2, select.getFields().size());
        Assert.assertEquals("Invalid node name.", "CODE", select.getFields().get(0).getName());
        Assert.assertEquals("Invalid node alias.", "cod", select.getFields().get(0).getAlias());

        Assert.assertEquals("Invalid node name.", "state", ((FieldNode) select.getFields().get(1)).getTableName());
        Assert.assertEquals("Invalid node name.", "NAME", select.getFields().get(1).getName());
        Assert.assertEquals("Invalid node alias.", "name", select.getFields().get(1).getAlias());

        Assert.assertEquals("Invalid node size.", 2, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
        Assert.assertEquals("Invalid node alias.", "state", select.getTables().get(1).getName());
    }

    /**
     * Test tables with alias.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testTwoTableWithAlias() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select *, name FROM client as cli, state STATE");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 2, select.getFields().size());
        Assert.assertEquals("Invalid node size.", 2, select.getTables().size());

        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
        Assert.assertEquals("Invalid node alias.", "cli", select.getTables().get(0).getAlias());
        Assert.assertEquals("Invalid node name.", "state", select.getTables().get(1).getName());
        Assert.assertEquals("Invalid node alias.", "STATE", select.getTables().get(1).getAlias());
    }

    /**
     * Test for where token.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testWhere() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT * FROM client as test WHERE a = b and c <> t");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 1, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
        Assert.assertEquals("Invalid node alias.", "test", select.getTables().get(0).getAlias());

        Assert.assertNotNull("Invalid node value.", select.getCondition());
        Assert.assertTrue("Invalid node type.", select.getCondition() instanceof ANDNode);

        ANDNode node = ((ANDNode) select.getCondition());
        Assert.assertEquals("Invalid node size.", 2, node.getChildren().size());
        Assert.assertTrue("Invalid node type.", node.getChildren().get(0) instanceof EqualsNode);
        Assert.assertTrue("Invalid node type.", node.getChildren().get(1) instanceof NotEqualsNode);
        Assert.assertEquals("Invalid node name.", "a",
                ((EqualsNode) node.getChildren().get(0)).getField().getName());
        Assert.assertEquals("Invalid node name.", "b",
                ((EqualsNode) node.getChildren().get(0)).getLast().getName());
        Assert.assertEquals("Invalid node name.", "c",
                ((NotEqualsNode) node.getChildren().get(1)).getField().getName());
        Assert.assertEquals("Invalid node name.", "t",
                ((NotEqualsNode) node.getChildren().get(1)).getLast().getName());
    }

    /**
     * Test a where with alias.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testWhereWithAlias() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "SELECT * FROM client as test WHERE test.a = c.b");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", TokenType.ASTERISK.name(), select.getFields().get(0).getName());

        Assert.assertEquals("Invalid node size.", 1, select.getTables().size());
        Assert.assertEquals("Invalid node name.", "client", select.getTables().get(0).getName());
        Assert.assertEquals("Invalid node alias.", "test", select.getTables().get(0).getAlias());

        Assert.assertNotNull("Invalid node value.", select.getCondition());
        Assert.assertTrue("Invalid node type.", select.getCondition() instanceof EqualsNode);

        EqualsNode node = ((EqualsNode) select.getCondition());
        Assert.assertEquals("Invalid node table name.", "test", node.getField().getTableName());
        Assert.assertEquals("Invalid node name.", "a", node.getField().getName());
        Assert.assertEquals("Invalid node table name.", "c", node.getLast().getTableName());
        Assert.assertEquals("Invalid node name.", "b", node.getLast().getName());
    }

    /**
     * Test for SQL exceptions.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testException() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select a. FROM AREACODES a");
        Assert.assertThrows("Invalid result", SQLException.class, parser::parse);
    }

    /**
     * Test for SELECT without FROM (two arguments).
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSelectWithoutFromTwoArguments() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select 1, 'b'");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 2, select.getFields().size());
        Assert.assertEquals("Invalid node name.", "1", select.getFields().get(0).getName());
        Assert.assertEquals("Invalid node name.", "b", select.getFields().get(1).getName());
    }

    /**
     * Test for SELECT without FROM.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSelectWithoutFrom() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select 1");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertEquals("Invalid node name.", "1", select.getFields().get(0).getName());
    }

    /**
     * Test for only SELECT token.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testSelectToken() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 0, select.getFields().size());
    }

    /**
     * Test for JOIN optimization in AND node.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testJoinOptimizationAND() throws SQLException {
        final SQLParser parser = new SQLParser(conn,
                "select * from geog.tblAC ac, geog.tblsttes st, geog.County c " +
                        " where c.StateID = st.State and st.State = ac.State and c.CountyID = 201");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertTrue("Invalid conditional type", select.getCondition() instanceof ANDNode);

        final ANDNode and = (ANDNode) select.getCondition();
        Assert.assertEquals("Invalid node size.", 3, and.getChildren().size());
    }

    /**
     * Test for JOIN optimization OR node.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testJoinOptimizationOR() throws SQLException {
        final SQLParser parser = new SQLParser(conn,
                "select * from geog.tblAC ac, geog.tblsttes st, geog.County c " +
                        " where c.StateID = st.State or st.State = ac.State or c.CountyID = 201");
        final List<StatementNode> list = parser.parse();
        final SQLNode tree = list.get(0);

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertTrue("Invalid conditional type", select.getCondition() instanceof ORNode);

        final ORNode and = (ORNode) select.getCondition();
        Assert.assertEquals("Invalid node size.", 3, and.getChildren().size());
    }

    /**
     * Test for parameters.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testParameters() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select * from geog.tblAC ac" +
                " where ac.State = ? and ? = ac.AreaCode");
        final List<StatementNode> list = parser.parse();
        final StatementNode tree = list.get(0);
        Assert.assertEquals("Invalid parameter count.", 2, tree.getParameterCount());

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertTrue("Invalid conditional type", select.getCondition() instanceof ANDNode);

        final ANDNode and = (ANDNode) select.getCondition();
        Assert.assertEquals("Invalid node size.", 2, and.getChildren().size());

        Assert.assertTrue("Invalid conditional type", and.getChildren().get(0) instanceof EqualsNode);
        EqualsNode equals = (EqualsNode) and.getChildren().get(0);

        Assert.assertEquals("Invalid node value.", "ac.State", equals.getField().toString());
        Assert.assertTrue("Invalid node type.", equals.getLast() instanceof ParameterNode);
        Assert.assertEquals("Invalid parameter index.", 0, ((ParameterNode) equals.getLast()).getParameterIndex());

        Assert.assertTrue("Invalid conditional type", and.getChildren().get(1) instanceof EqualsNode);
        equals = (EqualsNode) and.getChildren().get(1);
        Assert.assertTrue("Invalid node type.", equals.getField() instanceof ParameterNode);
        Assert.assertEquals("Invalid parameter index.", 1, ((ParameterNode) equals.getField()).getParameterIndex());
        Assert.assertEquals("Invalid node value.", "ac.AreaCode", equals.getLast().toString());
    }

    /**
     * Test for invalid join node.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testInvalidJoinNode() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select * from a where ab = 1 aaa ba = 2");
        Assert.assertThrows("Invalid join node", ParadoxSyntaxErrorException.class, parser::parse);
    }

    /**
     * Test for parenthesis.
     *
     * @throws SQLException in case of failures.
     */
    @Test
    public void testParenthesis() throws SQLException {
        final SQLParser parser = new SQLParser(conn, "select * from geog.tblAC ac" +
                " where (ac.State = ? and ? = ac.AreaCode)");
        final List<StatementNode> list = parser.parse();
        final StatementNode tree = list.get(0);
        Assert.assertEquals("Invalid parameter count.", 2, tree.getParameterCount());

        Assert.assertTrue("Invalid node type.", tree instanceof SelectNode);

        final SelectNode select = (SelectNode) tree;

        Assert.assertEquals("Invalid node size.", 1, select.getFields().size());
        Assert.assertTrue("Invalid conditional type", select.getCondition() instanceof ANDNode);

        final ANDNode and = (ANDNode) select.getCondition();
        Assert.assertEquals("Invalid node size.", 2, and.getChildren().size());

        Assert.assertTrue("Invalid conditional type", and.getChildren().get(0) instanceof EqualsNode);
        EqualsNode equals = (EqualsNode) and.getChildren().get(0);

        Assert.assertEquals("Invalid node value.", "ac.State", equals.getField().toString());
        Assert.assertTrue("Invalid node type.", equals.getLast() instanceof ParameterNode);
        Assert.assertEquals("Invalid parameter index.", 0, ((ParameterNode) equals.getLast()).getParameterIndex());

        Assert.assertTrue("Invalid conditional type", and.getChildren().get(1) instanceof EqualsNode);
        equals = (EqualsNode) and.getChildren().get(1);
        Assert.assertTrue("Invalid node type.", equals.getField() instanceof ParameterNode);
        Assert.assertEquals("Invalid parameter index.", 1, ((ParameterNode) equals.getField()).getParameterIndex());
        Assert.assertEquals("Invalid node value.", "ac.AreaCode", equals.getLast().toString());
    }
}
