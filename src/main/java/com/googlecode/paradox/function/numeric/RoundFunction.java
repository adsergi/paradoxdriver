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
package com.googlecode.paradox.function.numeric;

import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.exceptions.ParadoxSyntaxErrorException;
import com.googlecode.paradox.function.FunctionType;
import com.googlecode.paradox.function.IFunction;
import com.googlecode.paradox.parser.nodes.AsteriskNode;
import com.googlecode.paradox.parser.nodes.SQLNode;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.rowset.ValuesConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * The SQL ROUND functions.
 *
 * @version 1.0
 * @since 1.6.0
 */
public class RoundFunction implements IFunction {

    /**
     * The function name.
     */
    public static final String NAME = "ROUND";

    @Override
    public String remarks() {
        return "Returns a number to a specified number of decimal places.";
    }

    @Override
    public Column[] getColumns() {
        return new Column[]{
                new Column(null, ParadoxType.NUMBER, 8, 15,
                        "The rounded number.", 0, false,
                        DatabaseMetaData.functionColumnResult),
                new Column("number", ParadoxType.NUMBER, 8, 15, "The number to be rounded.", 1,
                        false, DatabaseMetaData.functionColumnIn),
                new Column("decimal", ParadoxType.INTEGER, 0, 4, "The number of decimal places to round number to.", 2,
                        false, DatabaseMetaData.functionColumnIn),
                new Column("operation", ParadoxType.BOOLEAN, 0, 1, "If true, it rounds the result to the number of " +
                        "decimal, otherwise it truncates the result to the number of decimals. " +
                        "Default value is false.", 3, true, DatabaseMetaData.functionColumnIn)
        };
    }

    @Override
    public FunctionType type() {
        return FunctionType.NUMERIC;
    }

    @Override
    public ParadoxType fieldType() {
        return ParadoxType.NUMBER;
    }

    @Override
    public int parameterCount() {
        return 2;
    }

    @Override
    public boolean isVariableParameters() {
        return true;
    }

    @Override
    public Object execute(final ParadoxConnection connection, final Object[] values, final ParadoxType[] types,
                          final FieldNode[] fields) {
        final BigDecimal value = ValuesConverter.getBigDecimal(values[0]);
        final Integer decimal = ValuesConverter.getInteger(values[1]);

        if (value == null || decimal == null) {
            return null;
        }

        Boolean rounding = null;
        if (values.length == 3) {
            rounding = ValuesConverter.getBoolean(values[2]);
        }

        RoundingMode mode = RoundingMode.HALF_UP;
        if (rounding != null && rounding) {
            mode = RoundingMode.FLOOR;
        }
        return value.setScale(decimal, mode);
    }

    @Override
    public void validate(List<SQLNode> parameters) throws ParadoxSyntaxErrorException {
        for (final SQLNode node : parameters) {
            if (node instanceof AsteriskNode) {
                throw new ParadoxSyntaxErrorException(ParadoxSyntaxErrorException.Error.ASTERISK_IN_FUNCTION,
                        node.getPosition());
            }
        }

        if (parameters.size() > 3) {
            throw new ParadoxSyntaxErrorException(ParadoxSyntaxErrorException.Error.INVALID_PARAMETER_COUNT, 3);
        }
    }
}
