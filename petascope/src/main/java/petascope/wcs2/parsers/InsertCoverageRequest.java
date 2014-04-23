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

package petascope.wcs2.parsers;

import petascope.util.KVPSymbols;
import petascope.util.ListUtil;

/**
 *
 * @author rasdaman
 */
public class InsertCoverageRequest extends BaseRequest {
    
    /*WCS InsertCoverage request constant*/
    public static final String VALUE_INSERTCOVERAGE = "InsertCoverage";
    public static final String EXISTING = "existing";
    public static final String NEW = "new";
        
    private final String coverageRef;
    private String useId;
    
    //to-do
    //AbstractCoverage coverage;
    
    public InsertCoverageRequest(String url) {
        this(url, EXISTING);
    }
    
    public InsertCoverageRequest(String url, String useId) {
        this.coverageRef = url;
        this.useId = (useId == NEW) ? NEW : EXISTING;
    }
    
    public String getCoverageRef() {
        return this.coverageRef;
    }
    
      @Override
    public String toString() {
        return VALUE_INSERTCOVERAGE + ": " + this.coverageRef;
    }
}
