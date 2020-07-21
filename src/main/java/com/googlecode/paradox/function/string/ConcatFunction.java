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
package com.googlecode.paradox.function.string;

import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.function.FunctionType;
import com.googlecode.paradox.function.IFunction;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.utils.Constants;

import java.sql.DatabaseMetaData;

/**
 * The SQL CONCAT function.
 *
 * @version 1.1
 * @since 1.6.0
 */
public class ConcatFunction implements IFunction {

    /**
     * The function name.
     */
    public static final String NAME = "CONCAT";

    @Override
    public String remarks() {
        return "Concatenate a sequence of strings. This functions support any number of parameters.";
    }

    @Override
    public Column[] getColumns() {
        return new Column[]{
                new Column(null, ParadoxType.VARCHAR, 0, Constants.MAX_STRING_SIZE,
                        "The concatenated string.", 0, true, DatabaseMetaData.functionColumnResult),
                new Column("value1", ParadoxType.VARCHAR, 0, Constants.MAX_STRING_SIZE,
                        "The string to concatenate", 1, true, DatabaseMetaData.functionColumnIn),
                new Column("value2", ParadoxType.VARCHAR, 0, Constants.MAX_STRING_SIZE,
                        "The string to concatenate", 2, true, DatabaseMetaData.functionColumnIn)
        };
    }

    @Override
    public FunctionType type() {
        return FunctionType.STRING;
    }

    @Override
    public ParadoxType fieldType() {
        return ParadoxType.VARCHAR;
    }

    @Override
    public int parameterCount() {
        return 0;
    }

    @Override
    public boolean isVariableParameters() {
        return true;
    }

    @Override
    public Object execute(final ParadoxConnection connection, final Object[] values, final ParadoxType[] types,
                          final FieldNode[] fields) {
        final StringBuilder ret = new StringBuilder();
        for (final Object value : values) {
            if (value != null) {
                ret.append(value);
            }
        }

        return ret.toString();
    }
}
