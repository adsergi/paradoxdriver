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
import com.googlecode.paradox.exceptions.ParadoxSyntaxErrorException;
import com.googlecode.paradox.function.FunctionType;
import com.googlecode.paradox.function.IFunction;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.rowset.ValuesConverter;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * The SQL SUBSTRING function.
 *
 * @version 1.2
 * @since 1.6.0
 */
public class SubstringFunction implements IFunction {

    /**
     * The function name.
     */
    public static final String NAME = "SUBSTRING";

    @Override
    public String remarks() {
        return "Extracts some characters from a string.";
    }

    @Override
    public Column[] getColumns() {
        return new Column[]{
                new Column(null, ParadoxType.VARCHAR, 255, 0, "The extracted string.", 0, true,
                        DatabaseMetaData.functionColumnResult),
                new Column("value", ParadoxType.VARCHAR, 255, 0, "The string to extract from.", 1, true,
                        DatabaseMetaData.functionColumnIn),
                new Column("start", ParadoxType.INTEGER, 8, 0, "The start position. Begin with 1.", 2, true,
                        DatabaseMetaData.functionColumnIn),
                new Column("length", ParadoxType.INTEGER, 8, 0, "The amount to extract.", 3, true,
                        DatabaseMetaData.functionColumnIn)
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
        return 3;
    }

    @Override
    public Object execute(final ParadoxConnection connection, final Object[] values, final ParadoxType[] types,
                          final FieldNode[] fields) throws SQLException {
        if (values[0] == null) {
            return null;
        }

        final int index = ValuesConverter.getPositiveInteger(values[1]) - 1;
        if (index == -1) {
            throw new ParadoxSyntaxErrorException(ParadoxSyntaxErrorException.Error.INVALID_PARAMETER_VALUE, values[1]);
        }

        final String value = values[0].toString();
        if (index >= value.length()) {
            return "";
        }

        final int length = ValuesConverter.getPositiveInteger(values[2]);
        if (index + length > value.length()) {
            return value.substring(index);
        }

        return value.substring(index, index + length);
    }
}
