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

package petascope.wcs2.handlers;

import java.io.IOException;
import java.util.List;
import nu.xom.Document;
import petascope.wcs2.parsers.InsertCoverageRequest;
import petascope.wcs2.handlers.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.core.DbMetadataSource;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCSException;
import static petascope.util.XMLSymbols.LABEL_COVERAGE_DESCRIPTIONS;
import static petascope.util.XMLSymbols.NAMESPACE_WCS;
import static petascope.util.XMLUtil.serialize;
import petascope.wcs2.extensions.FormatExtension;

/**
 *
 * @author rasdaman
 */
public class InsertCoverageHandler extends AbstractRequestHandler<InsertCoverageRequest> {
    
    private static Logger log = LoggerFactory.getLogger(InsertCoverageHandler.class);
    
    public InsertCoverageHandler(DbMetadataSource meta) {
        super(meta);
    }
    
    @Override
    public Response handle(InsertCoverageRequest request) throws WCSException {
        Document ret = constructDocument(LABEL_COVERAGE_DESCRIPTIONS, NAMESPACE_WCS);
        try {
            /*the response goes here.*/
            return new Response(null, serialize(ret), FormatExtension.MIME_XML);
        } catch (IOException ex) {
            throw new WCSException(ExceptionCode.IOConnectionError,
                    "Error serializing constructed document", ex);
        }
    }
}
