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
package com.googlecode.paradox.procedures.math;

import com.googlecode.paradox.procedures.AbstractCallableProcedure;

/**
 * The average procedure.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.1
 */
public final class Average extends AbstractCallableProcedure {
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public String getName() {
        return "average";
    }
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public String getRemarks() {
        return "Returns the average values.";
    }
}
