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

package petascope.wcsTransaction.parsers;

import java.util.List;
import java.util.Map;
import petascope.HTTPRequest;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCSException;
import static petascope.util.KVPSymbols.*;
import petascope.util.StringUtil;
import petascope.wcs2.handlers.RequestHandler;
import petascope.wcs2.parsers.KVPParser;

/**
 *
 * @author rasdaman
 */
public class KVPInsertCoverageParser extends KVPParser<InsertCoverageRequest> {
    
    @Override
    public InsertCoverageRequest parse(HTTPRequest request) throws WCSException {
        String input = request.getRequestString();
        Map<String, List<String>> p = StringUtil.parseQuery(input);
        checkEncodingSyntax(p, KEY_COVERAGEREF, KEY_VERSION, KEY_USEID);
        String coverageref = p.get(KEY_COVERAGEREF).toString();
        if (coverageref == null) {
            throw new WCSException(ExceptionCode.InvalidRequest, "A InsertCoverage request must specify a "+ KEY_COVERAGEREF + ".");
        }
        
        InsertCoverageRequest ret;
        
        if (p.containsKey(KEY_USEID)) {
            String useid = p.get(KEY_USEID).toString();
            ret = new InsertCoverageRequest(coverageref, useid);
        } else {
            ret = new InsertCoverageRequest(coverageref);
        }
        
        return ret;
    }
    
    @Override
    public String getOperationName() {
        return RequestHandler.INSERT_COVERAGE;
    }
}
