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

/**
 * Stores the driver constants.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.0
 */
public final class Constants {
    
    /**
     * Driver Name.
     */
    public static final String DRIVER_NAME = "Paradox";
    /**
     * Driver String Version.
     */
    public static final String DRIVER_VERSION = Constants.MAJOR_VERSION + "." + Constants.MINOR_VERSION;
    /**
     * Invalid input date.
     */
    public static final String ERROR_INVALID_DATE = "Invalid input date.";
    /**
     * Unsupported operation.
     */
    public static final String ERROR_UNSUPPORTED_OPERATION = "Unsupported operation.";
    /**
     * Major version of the Driver.
     */
    public static final int MAJOR_VERSION = 1;
    /**
     * Paradox max string size.
     */
    public static final int MAX_STRING_SIZE = 255;
    /**
     * Minor version of the Driver.
     */
    public static final int MINOR_VERSION = 3;
    /**
     * Driver prefix.
     */
    public static final String URL_PREFIX = "jdbc:paradox:";

    /**
     * Max buffer size.
     */
    public static final int MAX_BUFFER_SIZE = 2_048;

    /**
     * Utility class.
     */
    private Constants() {
        // Utility class.
    }
}
