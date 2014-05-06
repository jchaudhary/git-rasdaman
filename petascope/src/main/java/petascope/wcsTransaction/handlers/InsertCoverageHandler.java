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

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import petascope.wcsTransaction.parsers.InsertCoverageRequest;
import petascope.wcs2.handlers.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.core.CrsDefinition;
import petascope.core.DbMetadataSource;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCSException;
import static petascope.util.CrsUtil.*;
import static petascope.util.CrsUtil.getCrsUris;
import static petascope.util.ListUtil.ltos;
import petascope.util.Pair;
import petascope.util.StringUtil;
import static petascope.util.StringUtil.bufferedToString;
import static petascope.util.StringUtil.sdomToStringBuilder;
import static petascope.util.XMLSymbols.LABEL_COVERAGE_DESCRIPTIONS;
import static petascope.util.XMLSymbols.NAMESPACE_WCS;
import static petascope.util.XMLUtil.serialize;
import petascope.wcs2.extensions.FormatExtension;
import petascope.util.XMLUtil;
import static petascope.util.XMLUtil.CollectNamespaceAndPrefix;
import static petascope.util.XMLUtil.collectIds;
import static petascope.util.XMLUtil.returnAttributeValue;
import static petascope.util.XMLUtil.returnNamespaceUrl;
import static petascope.util.XMLUtil.returnStream;
import static petascope.util.XMLUtil.returnValue;
import petascope.wcps.server.core.CellDomainElement;
import petascope.wcps.server.core.DomainElement;
import petascope.wcs2.handlers.AbstractRequestHandler;
import petascope.wcs2.handlers.Response;

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
    public Response handle(InsertCoverageRequest request) throws WCSException, PetascopeException, SecoreException {
        Document ret = constructDocument(LABEL_COVERAGE_DESCRIPTIONS, NAMESPACE_WCS);
        Element root = null;
        //String url = request.getCoverageRef();
        String url = "http://localhost:8080/rasdaman/?service=WCS&version=2.0.1&request=GetCoverage&CoverageId=mean_summer_airtemp";
        Document coverage = null;
        Builder parser = new Builder();
        Set<Pair<String, String>> namespaceSet = new HashSet<Pair<String, String>>();
        try {
            coverage = parser.build(url);
            root = coverage.getRootElement();
            namespaceSet = CollectNamespaceAndPrefix(coverage, root);
            System.out.println(namespaceSet);
        } catch (ParsingException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
         
        //Preparing to insert into rasdaman
        List<Pair<Integer,Integer>> sdom = new ArrayList();
        Element domainSet = root.getFirstChildElement("domainSet", returnNamespaceUrl("gml", namespaceSet));
        String highTemp = returnValue(domainSet, "high");
        String lowTemp = returnValue(domainSet, "low");
        String dimension = returnAttributeValue(domainSet, "dimension");
        log.trace("dimension = " + Integer.valueOf(dimension));
        String sdomString = sdomToStringBuilder(Integer.valueOf(dimension), highTemp, lowTemp);
        log.trace(("Strng format " + sdomString));
        Element rangeSet = root.getFirstChildElement("rangeSet", returnNamespaceUrl("gml", namespaceSet));
        
        
        String collName = "multijava";
        String collType = "GreySet";
        String baseType = "char";
        meta.createCollection(collName, collType);
        meta.initializeCollection(collName,domainSet,baseType);
        meta.insertCoverage(collName, domainSet, rangeSet, baseType);
        
        
        //Element rangeSet = root.getFirstChildElement("rangeSet", returnNamespaceUrl("gml", namespaceSet));
        String cs = returnAttributeValue(rangeSet, "tupleList", "cs");
        log.trace(cs);
        String ts = returnAttributeValue(rangeSet, "tupleList", "ts");
        log.trace(ts);
      
        
       
        
        System.out.println("inserting into rasdaman portion"); 
        String testdata = "1,2,3,4,5,2,3,6,5,4,3,5,6,8,9";
        
       
        
       
        
        
        /*
        String gmlNamespace = returnNamespaceUrl("gml", namespaceSet);
        String coverageName = root.getAttributeValue("id", gmlNamespace);
        log.debug("Coverage name = " + coverageName);
        int coverageId = meta.getCoverageId(coverageName);
        String coverageType = root.getLocalName();
        log.debug("Coverage Type = " + coverageType);
        String nativeFormat = returnAttributeValue(root, "Point", "srsName");
        log.debug("Native  format : " + nativeFormat);
        
        List<String> crsUris = new ArrayList<String>();
        crsUris = getCrsUris(nativeFormat);
        log.debug("Crs Uris : " + crsUris);
        //for (int i =0; i <crsUris.size(); i++) {
          //  System.out.println(getGmlDefinition(crsUris.get(i)));
        //}
        
        Element boundedBy = root.getFirstChildElement("boundedBy", returnNamespaceUrl("gml", namespaceSet));
        String srsDimension = returnAttributeValue(boundedBy, "Envelope", "srsDimension");
        log.debug("srs Dimension : " + srsDimension);
        List<CellDomainElement> cellDomain = new ArrayList();
        String lowerCorner = returnValue(boundedBy, "lowerCorner");
        String upperCorner = returnValue(boundedBy, "upperCorner");
        for (int i=0; i < Integer.valueOf(srsDimension); i++) {
            String low = lowerCorner.split(" ")[i];
            String high = upperCorner.split(" ")[i];
            CellDomainElement cde = null;
            cde = new CellDomainElement(low, high, i);
            cellDomain.add(cde);
        }
        
        Element domainSet = root.getFirstChildElement("domainSet", returnNamespaceUrl("gml", namespaceSet));
        List<DomainElement> domain = new ArrayList();
        String label = returnValue(domainSet, "axisLabels");
        String high = returnValue(domainSet, "high");
        String low = returnValue(domainSet, "low");
        String uom = returnAttributeValue(domainSet, "Point", "uomLabels");
        String dimension = returnAttributeValue(domainSet, "dimension");
        log.trace("dimension : " + dimension);
        for (int i=0; i < Integer.valueOf(dimension); i++) {
            try {
                String nativeCrs = returnAttributeValue(domainSet, "Point", "srsName");
                List<String> crsSet = new ArrayList<String>();
                crsSet = getCrsUris(nativeCrs);
                String tempLabel = label.split(" ")[i];
                log.trace("label : " + tempLabel);
                DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
                df.setParseBigDecimal(true);
                String tempHigh = high.split(" ")[i];
                BigDecimal tempHighbd = (BigDecimal) df.parseObject(tempHigh);
                log.trace("high : "  + tempHighbd);
                String tempLow = low.split(" ")[i];
                BigDecimal tempLowbd = (BigDecimal) df.parseObject(tempLow);
                log.trace("low : " + tempLowbd);
                String tempUom = uom.split(" ")[i];
                log.trace("uom : " + tempUom);
                
                //DomainElement de = new DomainElement(tempLowbd, tempHighbd, tempLabel, " ", tempUom, nativeCrs, i, );
                //domain.add(de);
            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
               
        //BigInteger oid;
        //oid = meta.getCollOid("rgb");
        */
        try {
            /*the response goes here.*/
            return new Response(null, serialize(ret), FormatExtension.MIME_XML);
        } catch (IOException ex) {
            throw new WCSException(ExceptionCode.IOConnectionError,
                    "Error serializing constructed document", ex);
        }
    }
}
