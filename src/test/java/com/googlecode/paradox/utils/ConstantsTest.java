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

package com.googlecode.paradox.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link Constants} class.
 *
 * @version 1.1
 * @since 1.3
 */
public class ConstantsTest {

    /**
     * Test for sanity.
     */
    @Test
    public void testSanity() {
        Assert.assertTrue("Utility class in wrong format.", TestUtil.assertUtilityClassWellDefined(Constants.class));
    }
}
