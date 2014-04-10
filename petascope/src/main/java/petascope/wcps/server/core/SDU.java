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
package petascope.wcps.server.core;

import org.apache.commons.math.complex.Complex;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import petascope.util.WcpsConstants;

/**
 * This is a static utility class, which provides encoding and decoding of various datatypes
 * from/to strings. It also handles packing and unpacking of structures.
 */
public class SDU {

    public static String boolean2str(List<Boolean> boolea) {
        List<String> strings = new ArrayList<String>(boolea.size());
        Iterator<Boolean> i = boolea.iterator();

        while (i.hasNext()) {
            strings.add(i.next().toString());
        }

        return string2str(strings);

    }

    public static String complex2str(List<Complex> complex) {
        List<String> strings = new ArrayList<String>(complex.size());
        Iterator<Complex> i = complex.iterator();

        while (i.hasNext()) {
            Complex c = i.next();

            strings.add(c.getReal() + "#" + c.getImaginary());
        }

        return string2str(strings);

    }

    public static String double2str(List<Double> doubl) {
        List<String> strings = new ArrayList<String>(doubl.size());
        Iterator<Double> i = doubl.iterator();

        while (i.hasNext()) {
            strings.add(i.next().toString());
        }

        return string2str(strings);

    }

    public static String integer2str(List<BigInteger> integer) {
        List<String> strings = new ArrayList<String>(integer.size());
        Iterator<BigInteger> i = integer.iterator();

        while (i.hasNext()) {
            strings.add(i.next().toString());
        }

        return string2str(strings);

    }

    public static List<Boolean> str2boolean(String string) {
        List<String> strings = str2string(string);

        List<Boolean> booleans = new ArrayList<Boolean>(strings.size());
        Iterator<String> i = strings.iterator();

        while (i.hasNext()) {
            booleans.add(i.next().equals(WcpsConstants.MSG_TRUE));
        }

        return booleans;

    }

    public static List<Complex> str2complex(String string) {
        List<String> strings = str2string(string);

        List<Complex> complexes = new ArrayList<Complex>(strings.size());
        Iterator<String> i = strings.iterator();

        while (i.hasNext()) {
            String[] complex = i.next().split("#");

            complexes.add(new Complex(new Double(complex[0]), new Double(complex[1])));
        }

        return complexes;

    }

    public static List<Double> str2double(String string) {
        List<String> strings = str2string(string);

        List<Double> doubles = new ArrayList<Double>(strings.size());
        Iterator<String> i = strings.iterator();

        while (i.hasNext()) {
            doubles.add(new Double(i.next()));
        }

        return doubles;

    }

    public static List<BigInteger> str2integer(String string) {
        List<String> strings = str2string(string);

        List<BigInteger> integers = new ArrayList<BigInteger>(strings.size());
        Iterator<String> i = strings.iterator();

        while (i.hasNext()) {
            integers.add(new BigInteger(i.next()));
        }

        return integers;

    }

    public static List<String> str2string(String string) {
        List<String> strings;

        if (string.startsWith("{") && string.contains(",") && string.endsWith("}")) {
            String[] components = string.substring(1, string.length() - 1).split(",");

            strings = new ArrayList<String>(components.length);

            for (int i = 0; i < components.length; i++) {
                strings.add(new String(components[i]));
            }

            return strings;
        } else {
            strings = new ArrayList<String>(1);
            strings.add(new String(string));

            return strings;
        }

    }

    public static String string2str(List<String> strin) {
        if (strin == null) {
            return null;
        }
        if (strin.size() == 0) {
            return "";
        }

        String string = "{";
        Iterator<String> i = strin.iterator();

        while (i.hasNext()) {
            string += i.next() + (i.hasNext()
                    ? ","
                    : "}");
        }

        return string;

    }
}
