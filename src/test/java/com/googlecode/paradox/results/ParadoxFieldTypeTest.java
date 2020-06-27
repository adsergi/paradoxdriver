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
package com.googlecode.paradox.results;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Types;

/**
 * Unit test for {@link ParadoxFieldType} class.
 *
 * @author Leonardo  Costa
 * @version 1.1
 * @since 1.3
 */
public class ParadoxFieldTypeTest {

    /**
     * Test for invalid type.
     */
    @Test
    public void testGetType() {
        Assert.assertEquals("Invalid SQL type.", Types.OTHER, ParadoxFieldType.getSQLTypeByType(-1));
    }

    /**
     * Test for SQL type.
     */
    @Test
    public void testSQLType() {
        Assert.assertEquals("Test for get SQL type.", ParadoxFieldType.AUTO_INCREMENT.getSQLType(),
                ParadoxFieldType.getSQLTypeByType(ParadoxFieldType.AUTO_INCREMENT.getType()));
    }

}
