/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.util;

import java.util.HashMap;

/**
 *
 * @author rasdaman
 */
public class TypeConstants {

    /**
     *
     */
    public static HashMap<String, String> TYPE_CONSTANTS= new HashMap<String, String>();
    public TypeConstants() {
        TYPE_CONSTANTS.put("char", "c");
        TYPE_CONSTANTS.put("octet", "o");
        TYPE_CONSTANTS.put("short", "s");
        TYPE_CONSTANTS.put("unsigned short", "us");
        TYPE_CONSTANTS.put("long", "l");
        TYPE_CONSTANTS.put("unsigned long", "ul");
        TYPE_CONSTANTS.put("float", "f");
        TYPE_CONSTANTS.put("double", "d");
    }
    
    public static HashMap<String, String> getTypeConstants() {
        return TYPE_CONSTANTS;
    }
}
