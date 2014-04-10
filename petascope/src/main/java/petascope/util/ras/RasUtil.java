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

package petascope.util.ras;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.IOUtils;
import org.odmg.Database;
import org.odmg.ODMGException;
import org.odmg.OQLQuery;
import org.odmg.QueryException;
import org.odmg.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.ConfigManager;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.RasdamanException;
import petascope.exceptions.WCPSException;
import petascope.util.WcpsConstants;
import static petascope.util.ras.RasConstants.RASQL_VERSION;
import petascope.wcps.grammar.WCPSRequest;
import petascope.wcps.grammar.wcpsLexer;
import petascope.wcps.grammar.wcpsParser;
import petascope.wcps.server.core.ProcessCoveragesRequest;
import petascope.wcps.server.core.Wcps;
import rasj.RasImplementation;
import rasj.RasConnectionFailedException;

/**
 * Rasdaman utility classes - execute queries, etc.
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class RasUtil {

    private static final Logger log = LoggerFactory.getLogger(RasUtil.class);

    //Default time between re-connect attempts in seconds (If setting not found)
    private static final int DEFAULT_TIMEOUT = 5;

    //Default number of re-connect attempts  (If setting not fount)
    private static final int DEFAULT_RECONNECT_ATTEMPTS = 3;

    // Useful patterns to extract data from the ``--out string'' RasQL output
    private static final String VERSION_PATTERN = "rasdaman (\\S+)-\\S+ .*$"; // group _1_ is version

    /**
     * Execute a RasQL query with configured credentials.
     * @param query
     * @return
     * @throws RasdamanException
     */
    public static Object executeRasqlQuery(String query) throws RasdamanException {
        return executeRasqlQuery(query, ConfigManager.RASDAMAN_USER, ConfigManager.RASDAMAN_PASS);
    }

    /**
     * Execute a RasQL query with specified credentials.
     * @param query
     * @param username
     * @param password
     * @return
     * @throws RasdamanException
     */
    // FIXME - should return just String?
    public static Object executeRasqlQuery(String query, String username, String password) throws RasdamanException {
        RasImplementation impl = new RasImplementation(ConfigManager.RASDAMAN_URL);
        impl.setUserIdentification(username, password);
        Database db = impl.newDatabase();
	int maxAttempts, timeout, attempts = 0;

	//The result of the query will be assigned to ret
	//Should allways return a result (empty result possible)
	//since a RasdamanException will be thrown in case of error
	Object ret=null;

	try {
	    timeout = Integer.parseInt(ConfigManager.RASDAMAN_RETRY_TIMEOUT) * 1000;
	} catch(NumberFormatException ex) {
	    timeout = DEFAULT_TIMEOUT * 1000;
	    log.info("The setting " + ConfigManager.RASDAMAN_RETRY_TIMEOUT + " is not defined. Assuming " + DEFAULT_TIMEOUT + " seconds between re-connect attemtps to a rasdaman server.");
        }

        try {
            maxAttempts = Integer.parseInt(ConfigManager.RASDAMAN_RETRY_ATTEMPTS);
        } catch(NumberFormatException ex) {
            maxAttempts = DEFAULT_RECONNECT_ATTEMPTS;
            log.info("The setting " + ConfigManager.RASDAMAN_RETRY_ATTEMPTS + " is not defined. Assuming " + DEFAULT_RECONNECT_ATTEMPTS + " attempts to connect to a rasdaman server.");
        }

        Transaction tr;

        //Try to connect until the maximum number of attempts is reached
        //This loop handles connection attempts to a saturated rasdaman
        //complex which will refuse the connection until a server becomes
        //available.
        boolean queryCompleted = false, dbOpened = false;
        while(!queryCompleted) {

            //Try to obtain a free rasdaman server
            try {
                db.open(ConfigManager.RASDAMAN_DATABASE, Database.OPEN_READ_ONLY);
                dbOpened = true;
                tr = impl.newTransaction();
                tr.begin();
                OQLQuery q = impl.newOQLQuery();

                //A free rasdaman server was obtain, executing query
                try {
                    q.create(query);
                    log.trace("Executing query {}", query);
                    ret = q.execute();
                    tr.commit();
                    queryCompleted = true;
                } catch (QueryException ex) {

                    //Executing a rasdaman query failed
                    tr.abort();
                    throw new RasdamanException(ExceptionCode.RasdamanRequestFailed,
                            "Error evaluating rasdaman query: '" + query, ex);
                } catch (Error ex) {
                    tr.abort();
                    throw new RasdamanException(ExceptionCode.RasdamanRequestFailed,
                            "Requested more data than the server can handle at once.");
                } finally {

                    //Done connection with rasdaman, closing database.
                    try {
                        db.close();
                    } catch (ODMGException ex) {
                        log.info("Error closing database connection: ", ex);
                    }
                }
            } catch(RasConnectionFailedException ex) {

                //A connection with a Rasdaman server could not be established
                //retry shortly unless connection attpempts exceded the maximum
                //possible connection attempts.
                attempts++;
                if(dbOpened)
                    try {
                        db.close();
                    } catch(ODMGException e) {
                        log.info("Error closing database connection: ", e);
                    }
                dbOpened = false;
                if(!(attempts < maxAttempts))
                    //Throw a RasConnectionFailedException if the connection
                    //attempts exceeds the maximum connection attempts.
                    throw ex;

                //Sleep before trying to open another connection
                try {
                    Thread.sleep(timeout);
                } catch(InterruptedException e) {
                    log.error("Thread " + Thread.currentThread().getName() +
                            " was interrupted while searching a free server.");
                    throw new RasdamanException(ExceptionCode.RasdamanUnavailable,
                            "Unable to get a free rasdaman server.");
                }
            } catch(ODMGException ex) {

                //The maximum ammount of connection attempts was exceded
                //and a connection could not be established. Return
                //an exception indicating Rasdaman is unavailable.

                log.info("A Rasdaman request could not be fullfilled since no "+
                        "free Rasdaman server were available. Consider adjusting "+
                        "the values of rasdaman_retry_attempts and rasdaman_retry_timeout "+
                        "or adding more Rasdaman servers.",ex);

                throw new RasdamanException(ExceptionCode.RasdamanUnavailable,
                        "Unable to get a free rasdaman server.");
            }
        }
        return ret;
    }

    /**
     * Convert WCPS query in abstract syntax to a rasql query. This is done as
     * abstract -> XML -> rasql conversion.
     *
     * @param query WCPS query in abstract syntax
     * @param wcps WCPS engine
     * @return the corresponding rasql query
     * @throws WCPSException
     */
    public static String abstractWCPSToRasql(String query, Wcps wcps) throws WCPSException {
        if (query == null) {
            throw new WCPSException(ExceptionCode.InvalidParameterValue, "Can't convert null query");
        }
        log.trace("Converting abstract WCPS query\n{}", query);
        String xmlQuery = abstractWCPStoXML(query);
        try {
            String rasql = xmlWCPSToRasql(xmlQuery, wcps);
            log.debug("rasql: " + rasql);
            return rasql;
            //return xmlWCPSToRasql(xmlQuery, wcps);
        } catch (WCPSException ex) {
            throw ex;
        }
    }

    /**
     * Convert abstract WCPS query to XML syntax.
     *
     * @param query WCPS query in abstract syntax
     * @return the same query in XML
     * @throws WCPSException in case of error during the parsing/translation
     */
    public static String abstractWCPStoXML(String query) throws WCPSException {
        String ret = null;
        WCPSRequest request = null;
        try {
            CharStream cs = new ANTLRStringStream(query);
            wcpsLexer lexer = new wcpsLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream();
            tokens.setTokenSource(lexer);
            wcpsParser parser = new wcpsParser(tokens);

            log.trace("Parsing abstract WCPS query...");
            wcpsParser.wcpsRequest_return rrequest = parser.wcpsRequest();
            request = rrequest.value;
        } catch (RecognitionException ex) {
            throw new WCPSException(ExceptionCode.InternalComponentError,
                    "Error parsing abstract WCPS query.", ex);
        }

        try {
            log.trace("Converting parsed request to XML...");
            ret = request.toXML();
            log.debug("Done, xml query: " + ret);
        } catch (Exception ex) {
            throw new WCPSException(ExceptionCode.InternalComponentError,
                    "Error translating parsed abstract WCPS query to XML format.", ex);
        }
        return ret;
    }

    /**
     * Convert WCPS query in XML syntax to a rasql query.
     *
     * @param query WCPS query in XML syntax
     * @param wcps WCPS engine
     * @return the corresponding rasql query
     * @throws WCPSException
     */
    public static String xmlWCPSToRasql(String query, Wcps wcps) throws WCPSException {
        if (query == null) {
            throw new WCPSException(ExceptionCode.InvalidParameterValue, "Can't convert null query");
        }
        log.trace("Converting XML WCPS query\n{}", query);
        ProcessCoveragesRequest pcReq;
        try {
            pcReq = wcps.pcPrepare(ConfigManager.RASDAMAN_URL,
                    ConfigManager.RASDAMAN_DATABASE, IOUtils.toInputStream(query));
        } catch (WCPSException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WCPSException(ExceptionCode.InternalComponentError,
                    "Error translating XML WCPS query to rasql - " + ex.getMessage(), ex);
        }
        log.trace("Resulting RasQL query: [{}] {}", pcReq.getMime(), pcReq.getRasqlQuery());
        String ret = pcReq.getRasqlQuery();
        return ret;
    }

    /**
     * Execute a WCPS query given in abstract or XML syntax.
     *
     * @param query a WCPS query given in abstract syntax
     * @param wcps WCPS engine
     * @return result from executing query
     * @throws WCPSException
     * @throws RasdamanException
     */
    public static Object executeWcpsQuery(String query, Wcps wcps) throws WCPSException, RasdamanException {
        if (query == null) {
            throw new WCPSException(ExceptionCode.InvalidParameterValue, "Can't execute null query");
        }
        query = query.trim();
        log.trace("Executing WCPS query: {}", query);
        if (query.startsWith("<")) {
            return executeXmlWcpsQuery(query, wcps);
        } else {
            return executeAbstractWcpsQuery(query, wcps);
        }
    }

    /**
     * Execute a WCPS query given in abstract syntax.
     *
     * @param query a WCPS query given in abstract syntax
     * @param wcps WCPS engine
     * @return result from executing query
     * @throws WCPSException
     * @throws RasdamanException
     */
    public static Object executeAbstractWcpsQuery(String query, Wcps wcps) throws WCPSException, RasdamanException {
        if (query == null) {
            throw new WCPSException(ExceptionCode.InvalidParameterValue, "Can't execute null query");
        }
        log.trace("Executing abstract WCPS query");
        String rasquery = abstractWCPSToRasql(query, wcps);
        // Check if it is a rasql query
        if ( rasquery != null && rasquery.startsWith(WcpsConstants.MSG_SELECT) ) {
            return executeRasqlQuery(abstractWCPSToRasql(query, wcps));
        }
        return rasquery;

    }

    /**
     * Execute a WCPS query given in XML syntax.
     *
     * @param query a WCPS query given in XML syntax
     * @param wcps WCPS engine
     * @return the result from executing query
     * @throws WCPSException
     * @throws RasdamanException
     */
    public static Object executeXmlWcpsQuery(String query, Wcps wcps) throws WCPSException, RasdamanException {
        if (query == null) {
            throw new WCPSException(ExceptionCode.InvalidParameterValue, "Can't execute null query");
        }
        log.trace("Executing XML WCPS query");
        return executeRasqlQuery(xmlWCPSToRasql(query, wcps));
    }

    /**
     * Fetch rasdaman version by parsing RasQL ``version()'' output.
     *
     * @return The rasdaman version
     * @throws RasdamanException
     */
    public static String getRasdamanVersion() throws RasdamanException {

        String version = "";
        Object tmpResult = null;
        try {
            RasUtil.executeRasqlQuery("select " + RASQL_VERSION + "()");
        } catch (Exception ex) {
            log.warn("Failed retreiving rasdaman version", ex);
            version = "9.0";
        }

        if (null != tmpResult) {
            RasQueryResult queryResult = new RasQueryResult(tmpResult);

            // Some regexp to extract the version from the whole verbose output
            Pattern p = Pattern.compile(VERSION_PATTERN);
            Matcher m = p.matcher(queryResult.toString());

            if (m.find()) {
                version = m.group(1);
            }
        }

        log.debug("Read rasdaman version: \"" + version + "\"");
        return version;
    }
}
