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
/*
 * JOMDoc - A Java library for OMDoc documents (http://omdoc.org/jomdoc).
 * 
 * Original author    Dimitar Misev <d.misev@jacobs-university.de>
 * Web                http://kwarc.info/dmisev/
 * Created            Jul 13, 2008, 5:24:36 PM
 * 
 * Filename           $Id: TraversableXOM.java 1976 2010-07-31 12:07:20Z dmisev $
 * Revision           $Revision: 1976 $
 * Last modified on   $Date: 2010-07-31 14:07:20 +0200 (Sat, 31 Jul 2010) $
 *               by   $Author: dmisev $
 * 
 * Copyright (C) 2007,2008 the KWARC group (http://kwarc.info)
 * Licensed under the GNU Public License v3 (GPL3).
 * For other licensing contact Michael Kohlhase <m.kohlhase@jacobs-university.de>
 */
package petascope.util.traverse;

import nu.xom.Element;
import nu.xom.Node;

/**
 * An adapter for XOM <code>Elements</code>s
 * 
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class TraversableXOM implements Traversable<Node> {

    private Node n;

    public TraversableXOM(Node n) {
        this.n = n;
    }

    public boolean isElement() {
        return n instanceof Element;
    }

    public Node getValue() {
        return n;
    }

    public int childCount() {
        if (n != null) {
            return n.getChildCount();
        }
        return 0;
    }

    public Traversable child(int position) {
        if (n == null) {
            return null;
        }
        return new TraversableXOM(n.getChild(position));
    }

    public Traversable parent() {
        if (n == null) {
            return null;
        }
        return new TraversableXOM(n.getParent());
    }

    @Override
    public String toString() {
        if (n != null) {
            return n.toXML();
        }
        return "";
    }

    public boolean isTraversable() {
        return n != null && n instanceof Element;
    }

    public int indexOf(Traversable child) {
        if (isElement()) {
            return ((Element) n).indexOf((Node) child.getValue());
        }
        return -1;
    }
}
