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

import petascope.wcsTransaction.parsers.DeleteCoverageRequest;
import java.util.List;
import java.util.Map;
import petascope.HTTPRequest;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCSException;
import petascope.util.StringUtil;
import static petascope.util.KVPSymbols.*;
import petascope.wcs2.handlers.RequestHandler;
import petascope.wcs2.parsers.KVPParser;


/**
 *
 * @author rasdaman
 */
public class KVPDeleteCoverageParser extends KVPParser<DeleteCoverageRequest>{
    
    @Override
    public DeleteCoverageRequest parse(HTTPRequest request) throws WCSException {
        String input = request.getRequestString();
        Map<String, List<String>> p = StringUtil.parseQuery(input);
        checkEncodingSyntax(p, KEY_COVERAGEID, KEY_VERSION);
        DeleteCoverageRequest ret = new DeleteCoverageRequest();
        List<String> coverageIds = p.get(KEY_COVERAGEID);
        if (coverageIds == null || coverageIds.isEmpty()) {
            throw new WCSException(ExceptionCode.InvalidRequest, "A DeleteCoverage request must specify at least one "+ KEY_COVERAGEID + ".");
        }
        ret.getCoverageIds().addAll(coverageIds);
        return ret;
    }
    
    @Override
    public String getOperationName() {
        return RequestHandler.DELETE_COVERAGE;
    }
}
