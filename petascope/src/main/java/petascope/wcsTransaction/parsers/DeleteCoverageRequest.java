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

import java.util.ArrayList;
import java.util.List;
import petascope.util.KVPSymbols;
import petascope.util.ListUtil;
import petascope.wcs2.parsers.BaseRequest;

/**
 *
 * @author rasdaman
 */
public class DeleteCoverageRequest  extends BaseRequest {
    
    /*WCS DeleteCoverage request constant*/
    public static final String VALUE_DELETECOVERAGE = "DeleteCoverage";
    
    /**
     * List of Coverage Ids that we want to delete
     * Shall contain either contain at least one Id or more than one Id
     */
    private final List<String> coverageIds;
    
    public DeleteCoverageRequest() {
        this.coverageIds = new ArrayList<String>();
    }
    
    public List<String> getCoverageIds() {
        return this.coverageIds;
    }
    
  @Override
    public String toString() {
        return VALUE_DELETECOVERAGE + ": " +
                KVPSymbols.KEY_COVERAGEID + "s= " + ListUtil.ltos(coverageIds,",");
    }
}
