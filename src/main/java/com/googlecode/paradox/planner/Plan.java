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
package com.googlecode.paradox.planner;

import com.googlecode.paradox.ParadoxConnection;

import java.sql.SQLException;

/**
 * Used to creates and execute SQL plans.
 *
 * @version 1.1
 * @since 1.1
 */
public interface Plan {

    /**
     * Execute this plan.
     *
     * @param connection the Paradox connection.
     * @param maxRows    the limit of rows that can be loaded. Zero means no limit.
     * @throws SQLException in case of failures.
     */
    void execute(final ParadoxConnection connection, final int maxRows) throws SQLException;

    /**
     * Optimize the statement.
     */
    void compile();
}