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
package com.googlecode.paradox.function.date;

import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.function.FunctionType;
import com.googlecode.paradox.function.IFunction;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;

import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.util.TimeZone;

/**
 * The SQL CURRENT_DATE function.
 *
 * @version 1.4
 * @since 1.6.0
 */
public class CurrentDateFunction implements IFunction {

    /**
     * The function name.
     */
    public static final String NAME = "CURRENT_DATE";

    @Override
    public String remarks() {
    	return "Gets the current date.";
    }
    
    @Override
    public Column[] getColumns() {
        return new Column[]{
                new Column(null, ParadoxType.DATE, 8, 15, "The current date.", 0, false,
                        DatabaseMetaData.functionColumnResult)
        };
    }
    
    @Override
    public FunctionType type() {
        return FunctionType.TIME_DATE;
    }
    
    @Override
    public ParadoxType fieldType() {
        return ParadoxType.DATE;
    }

    @Override
    public int parameterCount() {
        return 0;
    }

    @Override
    public boolean isAllowAlias() {
        return true;
    }

    @Override
    public Object execute(final ParadoxConnection connection, final Object[] values, final ParadoxType[] types,
                          final FieldNode[] fields) {
        long time = System.currentTimeMillis();
        return new Date(time + connection.getTimeZone().getOffset(time) - TimeZone.getDefault().getOffset(time));
    }
}
