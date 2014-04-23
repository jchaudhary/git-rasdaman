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
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import petascope.wcs2.parsers.InsertCoverageRequest;
import petascope.wcs2.handlers.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.core.DbMetadataSource;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.WCSException;
import petascope.util.Pair;
import static petascope.util.XMLSymbols.LABEL_COVERAGE_DESCRIPTIONS;
import static petascope.util.XMLSymbols.NAMESPACE_WCS;
import static petascope.util.XMLUtil.serialize;
import petascope.wcs2.extensions.FormatExtension;
import petascope.util.XMLUtil;
import static petascope.util.XMLUtil.CollectNamespaceAndPrefix;
import static petascope.util.XMLUtil.returnNamespaceUrl;

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
    public Response handle(InsertCoverageRequest request) throws WCSException, PetascopeException {
        Document ret = constructDocument(LABEL_COVERAGE_DESCRIPTIONS, NAMESPACE_WCS);
        Element root = null;
        //String url = request.getCoverageRef();
        String url = "http://localhost:8080/rasdaman/?service=WCS&version=2.0.1&request=GetCoverage&CoverageId=mr";
        Document coverage = null;
        Builder parser = new Builder();
        Set<Pair<String, String>> namespaceSet = new HashSet<Pair<String, String>>();
        try {
            coverage = parser.build(url);
            root = coverage.getRootElement();
            namespaceSet = CollectNamespaceAndPrefix(coverage, root);
        } catch (ParsingException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String namespaceUrl = returnNamespaceUrl("gml", namespaceSet);
        String coverageName = root.getAttributeValue("id", namespaceUrl);
        log.debug("Coverage name = " + coverageName);
        int coverageId = meta.getCoverageId(coverageName);
        String coverageType = root.getLocalName();
        log.debug("Coverage Type = " + coverageType);
        
        
        
        
        //BigInteger oid;
        //oid = meta.getCollOid("rgb");
        
        try {
            /*the response goes here.*/
            return new Response(null, serialize(ret), FormatExtension.MIME_XML);
        } catch (IOException ex) {
            throw new WCSException(ExceptionCode.IOConnectionError,
                    "Error serializing constructed document", ex);
        }
    }
}
