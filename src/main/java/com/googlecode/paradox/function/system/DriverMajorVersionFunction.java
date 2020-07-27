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
package com.googlecode.paradox.function.system;

import com.googlecode.paradox.ConnectionInfo;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.utils.Constants;

/**
 * The SQL DRIVER_MAJOR_VERSION functions.
 *
 * @version 1.2
 * @since 1.6.0
 */
public class DriverMajorVersionFunction extends AbstractSystemFunction {

    /**
     * The function name.
     */
    public static final String NAME = "DRIVER_MAJOR_VERSION";

    /**
     * Column parameter list.
     */
    private static final Column[] COLUMNS = {
            new Column(null, ParadoxType.INTEGER, "The driver major version.", 0, false, RESULT)
    };

    @Override
    public String getRemarks() {
        return "Gets the driver major version.";
    }

    @Override
    public Column[] getColumns() {
        return COLUMNS;
    }

    @Override
    public Object execute(final ConnectionInfo connectionInfo, final Object[] values, final ParadoxType[] types,
                          final FieldNode[] fields) {
        return Constants.MAJOR_VERSION;
    }
}
