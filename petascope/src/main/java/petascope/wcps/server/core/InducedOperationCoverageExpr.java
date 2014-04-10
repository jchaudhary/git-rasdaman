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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCPSException;
import petascope.util.WcpsConstants;

public class InducedOperationCoverageExpr extends AbstractRasNode implements ICoverageInfo {
    
    private static Logger log = LoggerFactory.getLogger(InducedOperationCoverageExpr.class);

    private IRasNode child;
    private CoverageInfo info;
    private String operation = "";

    public InducedOperationCoverageExpr(Node node, XmlQuery xq) throws WCPSException, SecoreException {
        String nodeName = node.getNodeName();
        
        log.trace(nodeName);

        if (nodeName.equals(WcpsConstants.MSG_RANGE_CONSTRUCTOR)) {
            operation = nodeName;
            child = new RangeCoverageExpr(node, xq);
            info = new CoverageInfo((((ICoverageInfo) child).getCoverageInfo()));
        } else {    // Try one of the groups
            child = null;

            if (UnaryOperationCoverageExpr.NODE_NAMES.contains(nodeName)) {
                try {
                    child = new UnaryOperationCoverageExpr(node, xq);
                    info = new CoverageInfo((((ICoverageInfo) child).getCoverageInfo()));
                    log.trace("induced Operation SUCCESS: " + node.getNodeName());
                } catch (WCPSException e) {
                    child = null;
                    if (e.getMessage().equals("Method not implemented")) {
                        throw e;
                    }
                }
            }

            if (BinaryOperationCoverageExpr.NODE_NAMES.contains(nodeName)) {
                try {
                    child = new BinaryOperationCoverageExpr(node, xq);
                    info = new CoverageInfo((((ICoverageInfo) child).getCoverageInfo()));
                    log.trace("Binary operation SUCCESS: " + node.getNodeName());
                } catch (WCPSException e) {
                    child = null;
                }
            }

            if (child == null) {
                throw new WCPSException("Invalid induced coverage expression, next node: " + node.getNodeName());
            } else {
                // Keep the child to let the XML tree be traversed
                super.children.add(child);
            }
        }

    }

    public CoverageInfo getCoverageInfo() {
        return info;
    }

    public String toRasQL() {
        return child.toRasQL();
    }
}
