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

import petascope.wcsTtransaction.parsers.KVPDeleteCoverageParser;
import petascope.wcsTtransaction.parsers.KVPInsertCoverageParser;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.HTTPRequest;

/**
 * Parsers are managed in this class.
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class ParsersRegistry {

    private static final Logger log = LoggerFactory.getLogger(ParsersRegistry.class);

    private static final Set<RequestParser> parsers = new HashSet<RequestParser>();

    static {
        initialize();
    }

    /**
     * Initialize registry: load available protocol binding extensions
     */
    public static void initialize() {
        registerParser(new XMLGetCapabilitiesParser());
        registerParser(new XMLDescribeCoverageParser());
        registerParser(new XMLGetCoverageParser());
        registerParser(new XMLProcessCoverageParser());
        registerParser(new KVPGetCapabilitiesParser());
        registerParser(new KVPDescribeCoverageParser());
        registerParser(new KVPGetCoverageParser());
        registerParser(new KVPProcessCoverageParser());
        registerParser(new KVPDeleteCoverageParser());
        registerParser(new KVPInsertCoverageParser());
        registerParser(new RESTGetCapabilitiesParser());
        registerParser(new RESTDescribeCoverageParser());
        registerParser(new RESTGetCoverageParser());
    }

    /**
     * Add new binding to the registry.
     * It will replace any other binding which has the same operation name and extension identifier.
     *
     * @param parser
     */
    public static void registerParser(RequestParser parser) {
        log.info("Registered parser {}", parser.getClass().getName());
        parsers.add(parser);
    }

    /**
     * @param request
     * @return a binding for the specified operation, that can parse the specified input, or null otherwise
     */
    public static RequestParser getParser(HTTPRequest request) {
        for (RequestParser parser : parsers) {
            if (parser.canParse(request)) {
                return parser;
            }
        }
        return null;
    }

    public static Set<RequestParser> getRegisteredParsers() {
        return parsers;
    }
}
