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
package com.googlecode.paradox.data.field;

import com.googlecode.paradox.data.ParadoxBuffer;
import com.googlecode.paradox.data.table.value.FieldValue;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Unit test for {@link TimestampField} class.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.3
 */
public class TimestampFieldTest {
    /**
     * Test for invalid match.
     */
    @Test
    public void testInvalidMatch() {
        final TimestampField field = new TimestampField();
        Assert.assertFalse("Invalid field value.", field.match(0));
    }

    /**
     * Test for parse method.
     *
     * @throws SQLException in case of parse errors.
     */
    @Test
    public void testParse() throws SQLException {
        final Calendar calendar = new GregorianCalendar(2013, 10, 24, 9, 29, 31);
        final Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());

        final TimestampField field = new TimestampField();
        final ParadoxBuffer buffer = new ParadoxBuffer(new byte[]{
                (byte) 0xC2, (byte) 0xCC, (byte) 0xE2, (byte) 0xD0,
                (byte) 0x99, (byte) 0x2A, (byte) 0xBC, (byte) 0x0F
        });
        final FieldValue value = field.parse(null, buffer, null);
        Assert.assertEquals("Invalid timestamp value.", timestamp, value.getTimestamp());
    }

    /**
     * Test for valid match.
     */
    @Test
    public void testValidMatch() {
        final TimestampField field = new TimestampField();
        Assert.assertTrue("Invalid field type.", field.match(0x15));
    }
}
