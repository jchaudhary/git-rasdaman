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

package petascope.core;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.ConfigManager;
import static petascope.ConfigManager.ENABLE_OWS_METADATA;
import static petascope.ConfigManager.ENABLE_OWS_METADATA_F;
import static petascope.ConfigManager.KEY_ENABLE_OWS_METADATA;
import static petascope.ConfigManager.METADATA_URL;
import static petascope.ConfigManager.SECORE_URLS;
import static petascope.ConfigManager.SECORE_URL_KEYWORD;
import static petascope.ConfigManager.SETTINGS_FILE;
import static petascope.core.DbMetadataSource.TABLE_RASDAMAN_COLLECTION;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.RasdamanException;
import petascope.exceptions.SecoreException;
import petascope.exceptions.WCSException;
import petascope.ows.Description;
import petascope.ows.ServiceProvider;
import petascope.swe.datamodel.AllowedValues;
import petascope.swe.datamodel.NilValue;
import petascope.swe.datamodel.Quantity;
import petascope.swe.datamodel.RealPair;
import petascope.util.CrsUtil;
import petascope.util.ListUtil;
import petascope.util.Pair;
import petascope.util.Vectors;
import petascope.util.WcsUtil;
import static petascope.util.ras.RasConstants.*;
import petascope.util.ras.RasQueryResult;
import petascope.util.ras.RasUtil;
import petascope.wcps.server.core.*;
import petascope.wcs2.parsers.BaseRequest;

/**
 *
 * @author rasdaman
 */
public class Test {
    
    private static Logger log = LoggerFactory.getLogger(DbMetadataSource.class);
    
    public Pair<String, String> getIndexDomain(String collName, BigInteger collOid, int rasdamanAxisOrder) throws PetascopeException {

    // Run RasQL query
    Object obj = null;
    String rasQuery =
            RASQL_SELECT + " " + RASQL_SDOM + "(c)["   + rasdamanAxisOrder + "]" +
            RASQL_FROM   + " " + collName + " " + RASQL_AS + " c " +
            RASQL_WHERE  + " " + RASQL_OID + "(c) = " + collOid
            ;
    log.debug("RasQL query : " + rasQuery);
    try {
        obj = RasUtil.executeRasqlQuery(rasQuery);
    } catch (RasdamanException ex) {
        throw new PetascopeException(ExceptionCode.InternalComponentError, "Error while executing RasQL query", ex);
    }

    // Parse the result
    Pair<String, String> bounds = Pair.of("","");
    if (obj != null) {
        RasQueryResult res = new RasQueryResult(obj);
        if (!res.getScalars().isEmpty()) {
            // TODO: can be done better with Minterval instead of sdom2bounds
            bounds = Pair.of(
                    res.getScalars().get(0).split(":")[0],
                    res.getScalars().get(0).split(":")[1]);
        } else {
            log.error("Marray " + collOid + " of collection " + collName + " was not found.");
            throw new PetascopeException(ExceptionCode.InvalidCoverageConfiguration,
                    "Marray " + collOid + " of collection " + collName + " was not found: wrong OID in " + TABLE_RASDAMAN_COLLECTION + "?");
        }
    } else {
        log.error("Empty response from rasdaman.");
        throw new PetascopeException(ExceptionCode.RasdamanError, "Empty response from rasdaman.");
    }
    System.out.println(bounds);
    return bounds;
    }
        
    public static void main(String [] args) throws PetascopeException {
        Test t = new Test();
        Pair<String, String> bounds;
        bounds = t.getIndexDomain("mr", BigInteger.valueOf(6145), 0);
        System.out.println(bounds);
    }

}
