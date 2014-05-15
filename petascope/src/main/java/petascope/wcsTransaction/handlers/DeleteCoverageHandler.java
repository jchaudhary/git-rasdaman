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

package petascope.wcsTransaction.handlers;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import nu.xom.Document;
import petascope.wcsTransaction.parsers.DeleteCoverageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.core.DbMetadataSource;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCSException;
import static petascope.util.XMLSymbols.LABEL_COVERAGE_DESCRIPTIONS;
import static petascope.util.XMLSymbols.NAMESPACE_WCS;
import static petascope.util.XMLUtil.serialize;
import petascope.wcs2.extensions.ExtensionsRegistry;
import petascope.wcs2.extensions.FormatExtension;
import petascope.wcs2.handlers.AbstractRequestHandler;
import petascope.wcs2.handlers.Response;
import petascope.wcs2.parsers.GetCoverageRequest;
/**
 *
 * @author rasdaman
 */
public class DeleteCoverageHandler extends AbstractRequestHandler<DeleteCoverageRequest> {
    
    private static Logger log = LoggerFactory.getLogger(DeleteCoverageHandler.class);
    
    public DeleteCoverageHandler(DbMetadataSource meta) {
        super(meta);
    }
    
    @Override
    public Response handle(DeleteCoverageRequest request) throws WCSException, PetascopeException  {
       Document ret = constructDocument(LABEL_COVERAGE_DESCRIPTIONS, NAMESPACE_WCS);
        try {
            List<String> coverageIds = request.getCoverageIds();
            meta.deleteCoverage(coverageIds, true);
            
            return new Response(null, serialize(ret), FormatExtension.MIME_XML);
        } catch (IOException ex) {
            throw new WCSException(ExceptionCode.IOConnectionError,
                    "Error serializing constructed document", ex);
        }
    }
}
