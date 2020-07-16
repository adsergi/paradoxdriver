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
package com.googlecode.paradox.data.filefilters;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

/**
 * Unit test for {@link ViewFilter}.
 *
 * @version 1.2
 * @since 1.0
 */
public class ViewFilterTest {

    /**
     * Test for acceptance.
     */
    @Test
    public void testAccept() {
        final File file = new File("test.qbe");
        final ViewFilter filter = new ViewFilter(Locale.ENGLISH, null);
        Assert.assertTrue("Invalid file filter.", filter.accept(file));
    }

}
