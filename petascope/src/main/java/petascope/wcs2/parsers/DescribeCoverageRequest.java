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

import java.util.ArrayList;
import java.util.List;
import petascope.util.ListUtil;
import petascope.util.KVPSymbols;

/**
 * A DescribeCoverage request object, populated by a parser.
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class DescribeCoverageRequest extends BaseRequest {

    /* WCS DescribeCoverage request constants */
    public static final String VALUE_DESCRIBECOVERAGE = "DescribeCoverage";

    private final List<String> coverageIds;

    public DescribeCoverageRequest() {
        coverageIds = new ArrayList<String>();
    }

    public List<String> getCoverageIds() {
        return coverageIds;
    }

    @Override
    public String toString() {
        return VALUE_DESCRIBECOVERAGE + ": " +
                KVPSymbols.KEY_COVERAGEID + "s=" + ListUtil.ltos(coverageIds,",");
    }
}
