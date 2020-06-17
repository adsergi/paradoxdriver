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
package com.googlecode.paradox.metadata;

import com.googlecode.paradox.ParadoxConnection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the paradox view data.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.0
 */
public final class ParadoxView extends ParadoxDataFile {

    /**
     * Stores the field list sort.
     */
    private List<ParadoxField> fieldsSort;

    /**
     * Creates a new instance.
     *
     * @param file       the file to read of.
     * @param name       the view name.
     * @param connection the database connection.
     */
    public ParadoxView(final File file, final String name, final ParadoxConnection connection) {
        super(file, name, connection);
    }

    /**
     * Gets the fields sort.
     *
     * @return the fieldsSort the fields sort.
     */
    public List<ParadoxField> getFieldsSort() {
        return Collections.unmodifiableList(this.fieldsSort);
    }

    /**
     * Sets the fields sort.
     *
     * @param fieldsSort the fields sort to set.
     */
    public void setFieldsSort(final List<ParadoxField> fieldsSort) {
        this.fieldsSort = new ArrayList<>(fieldsSort);
    }
}
