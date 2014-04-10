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

import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCPSException;
import petascope.util.WcpsConstants;
import static petascope.util.ras.RasConstants.*;

public class CondenseScalarExpr extends AbstractRasNode {
    
    private static Logger log = LoggerFactory.getLogger(CondenseScalarExpr.class);

    private CondenseOperation op;
    private Vector<AxisIterator> iterators;
    private IRasNode using;
    private IRasNode where;
    private String axisIteratorString;
    private String newIteratorName;

    public CondenseScalarExpr(Node node, XmlQuery xq) throws WCPSException, SecoreException {
        if (node.getNodeName().equals(WcpsConstants.MSG_CONDENSE)) {
            node = node.getFirstChild();
        }
        while ((node != null) && node.getNodeName().equals("#" + WcpsConstants.MSG_TEXT)) {
            node = node.getNextSibling();
        }
        
        log.trace(node.getNodeName());

        iterators = new Vector();
        newIteratorName = xq.registerNewExpressionWithVariables();

        while (node != null) {
            String name = node.getNodeName();
            if (op == null) {
                op = new CondenseOperation(node, xq);
            } else if (name.equals(WcpsConstants.MSG_ITERATOR)) {
                AxisIterator it = new AxisIterator(node.getFirstChild(), xq, newIteratorName);
                iterators.add(it);
                super.children.add(it);
            } else if (name.equals(WcpsConstants.MSG_WHERE)) {
                where = new BooleanScalarExpr(node.getFirstChild(), xq);
            } else {
                using = new CoverageExpr(node, xq);
            }

            node = node.getNextSibling();
            while ((node != null) && node.getNodeName().equals("#" + WcpsConstants.MSG_TEXT)) {
                node = node.getNextSibling();
            }
            
            // Keep the children to let XML tree be re-traversed
            if (where != null) super.children.add(where);
            if (using != null) super.children.add(using);
        }

        buildAxisIteratorDomain();
    }

    public String toRasQL() {
        String result = WcpsConstants.MSG_CONDENSE + " " + op.toRasQL() + " " + WcpsConstants.MSG_OVER + " ";
        result += axisIteratorString;
        if (where != null) {
            result += " " + WcpsConstants.MSG_WHERE + " " + where.toRasQL();
        }
        result += " " + WcpsConstants.MSG_USING + " " + using.toRasQL();
        return result;
    }

    /* Concatenates all the AxisIterators into one large multi-dimensional object,
     * that will be used to build to RasQL query */
    private void buildAxisIteratorDomain() {
        axisIteratorString = "";
        axisIteratorString += newIteratorName + " " + RASQL_IN + " [";

        for (int i = 0; i < iterators.size(); i++) {
            if (i > 0) {
                axisIteratorString += ", ";
            }
            AxisIterator ai = iterators.elementAt(i);
            axisIteratorString += ai.getInterval();
        }

        axisIteratorString += "]";
    }
}
