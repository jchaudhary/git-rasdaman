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
package petascope.wcps.grammar;

import petascope.util.WcpsConstants;

/**
 * RangeConstructorTerm, part of a RangeConstructorExpr
 *
 * @author Andrei Aiordachioaie
 */
public class RangeConstructorTerm implements IParseTreeNode {

    CoverageExpr expr;
    String field;

    public RangeConstructorTerm(String f, CoverageExpr c) {
        field = f;
        expr = c;
    }

    public String toXML() {
        String result = "";

        result += "<" + WcpsConstants.MSG_FIELD + ">" + field + "</" + WcpsConstants.MSG_FIELD + ">";
        result += expr.toXML();

        result = "<" + WcpsConstants.MSG_COMPONENT + ">" + result + "</" + WcpsConstants.MSG_COMPONENT + ">";
        return result;
    }
}
