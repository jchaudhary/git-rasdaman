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

import petascope.core.DbMetadataSource;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCSException;
import petascope.wcs2.extensions.ExtensionsRegistry;
import petascope.wcs2.extensions.FormatExtension;
import petascope.wcs2.parsers.GetCoverageRequest;

/**
 * GetCoverage operation for The Web Coverage Service 2.0
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class GetCoverageHandler extends AbstractRequestHandler<GetCoverageRequest> {

    public GetCoverageHandler(DbMetadataSource meta) {
        super(meta);
    }

    @Override
    public Response handle(GetCoverageRequest request)
            throws PetascopeException, WCSException, SecoreException {
        FormatExtension formatExtension = ExtensionsRegistry.getFormatExtension(request);
        System.out.println(formatExtension);
        if (formatExtension == null) {
            throw new WCSException(ExceptionCode.NoApplicableCode,
                    "No appropriate format extension was found that can handle the request for format : " + request.getFormat());
        }
        try {
            return formatExtension.handle(request, meta);
        } catch (PetascopeException ex) {
            throw ex;
        }
    }
}
