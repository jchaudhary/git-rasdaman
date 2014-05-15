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
import nu.xom.Elements;
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
import static petascope.util.XMLUtil.returnValueList;
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
        String url = "http://localhost:8080/rasdaman/?service=WCS&version=2.0.1&request=GetCoverage&CoverageId=mr";
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
        
        List<Pair<Integer,Integer>> sdom = new ArrayList();
        Element domainSet = root.getFirstChildElement("domainSet", returnNamespaceUrl("gml", namespaceSet));
        Element rangeSet = root.getFirstChildElement("rangeSet", returnNamespaceUrl("gml", namespaceSet));
        Element rangeType = root.getFirstChildElement("rangeType", returnNamespaceUrl("gml", namespaceSet));

        String gmlNamespace = returnNamespaceUrl("gml", namespaceSet);
        String coverageType = root.getLocalName();
        
        String useId = request.getUseId();
        int xa = useId.length();
        useId = useId.substring(1, xa-1);
        System.out.println(useId);
        String coverageName = "";
        if (useId.compareToIgnoreCase("existing") == 0) {
            coverageName = root.getAttributeValue("id", gmlNamespace);
            if (meta.existsCoverageName(coverageName)) {
                coverageName += "_v2";
            }
        } else if (useId.compareToIgnoreCase("new") == 0) {
            coverageName = root.getAttributeValue("id", gmlNamespace) + "_1";
        }
        
        //************************** Insert into rasdaman ****************************
        
        String collType = "GreySet"; //for mr
        meta.createCollection(coverageName, collType);
        
        String baseType = "char"; //for mr
        meta.initializeCollection(coverageName, domainSet, baseType);
        try {
            meta.insertCollection(coverageName, domainSet, rangeSet, baseType);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        //*********************************** insert into petascope *********************************
        int gmlSubtypeId = meta.getGmlSubtypeId(coverageType);
        String mimeType = "application/x-octet-stream";
        int nativeFormatId = meta.getMimeTypeId(mimeType);
        
        //meta.insertGeneralCoverageInfo(coverageName, gmlSubtypeId, nativeFormatId, true);
        
        int coverageId = meta.getCoverageId(coverageName);
        BigInteger collectionOid = meta.getCollOid(coverageName);
        int storageId = meta.getRasCollId(coverageName);
        //meta.insertIntoRasCollection(coverageName, collectionOid.intValue(), true);
        //meta.insertIntoRangeSet(coverageId, storageId, true);
        
        String rangeDataType = "unsigned char";
        int rangeDataTypeId = meta.getRangeDataTypeId(rangeDataType);
        
        int quantityId = meta.getQuantityId(rangeDataType);
        
        String rangeTypeComponentName = "value";
        
        //meta.insertIntoRangeTypeComponent(coverageId, rangeTypeComponentName, 0,rangeDataTypeId , quantityId, true);
        
        //String crsUri = 
        //meta.insertIntoCrsUri(crsUri, true);
        
        List<Integer> crsIds = new ArrayList<Integer>();
        crsIds.add(3);
        //meta.insertIntoDomainSet(coverageId, crsIds, true);
        String originString = returnValue(domainSet, "origin");
        System.out.println(originString);  //0 210
        //String x = originString.split(" ")[0];
        String x = "0";
        //String y = originString.split(" ")[1];
        String y = "210";
        int gridOriginX = Integer.parseInt(x);
        int gridOriginY = Integer.parseInt(y);
        //meta.insertIntoGriddedDomainSet(coverageId, gridOriginX, gridOriginY, true);
        
        //meta.insertIntoGridAxis(coverageId, 0, true);
        log.debug("rasdaman order 0 done.");
        //meta.insertIntoGridAxis(coverageId, 1, true);
        log.debug("rasdaman order 1 done.");
        
        List<String> offset = returnValueList(domainSet, "offsetVector");
        System.out.println(offset);
        
        int gridAxisId0 = meta.getGridAxisId(coverageId, 0);
        log.debug("rasdaman order 0 done.");
        int gridAxisId1 = meta.getGridAxisId(coverageId, 1);
        log.debug("rasdaman order 1 done.");
        
        //meta.insertIntoRectilinearAxis(gridAxisId0, 1, 0, true);
        log.debug("rasdaman order 0 done.");
        //meta.insertIntoRectilinearAxis(gridAxisId1, -1, 1, true);
        log.debug("rasdaman order 1 done.");
        
        String ows = "ows";
        String gmlcov =  "gmlcov";
        int metadataTypeId = 0;
        metadataTypeId = meta.getExtraMetadataTypeId(ows);
        String valueOws = "<test>ows</test>";
        String valueGmlCov = "<test>gmlcov</test>";
        //meta.insertIntoExtraMetadata(coverageId, metadataTypeId, valueOws, true);
        log.debug("metadata type Ows done");
        metadataTypeId = meta.getExtraMetadataTypeId(gmlcov);
        //meta.insertIntoExtraMetadata(coverageId, metadataTypeId, valueGmlCov, true);
        log.debug("metadata type gmlcov done");
        
        //String collName = "testhour";
        //String collType = "GreySet";
        //String baseType = "char";
        //meta.createCollection(collName, collType);
        //meta.initializeCollection(collName,domainSet,baseType);
        /*
        try {
            meta.insertCollection(collName, domainSet, rangeSet, baseType);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(InsertCoverageHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        //Element rangeSet = root.getFirstChildElement("rangeSet", returnNamespaceUrl("gml", namespaceSet));
        
        /*
        meta.createCollection("laddu", "GreySet");
        List<Pair<Integer, Integer>> coordinate = new ArrayList<Pair<Integer, Integer>>();
        Pair a= Pair.of("0", "885");
        Pair b = Pair.of("0", "710");
        coordinate.add(a);
        coordinate.add(b);
        meta.initialize2DCollection(coordinate, "laddu", "char");
        meta.insert2DCoverage("laddu", coordinate, returnValue(rangeSet, "tupleList"), baseType);
        */
               
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
