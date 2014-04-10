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
package petascope.wcs2.extensions;

import java.util.List;
import java.util.Map;
import petascope.HTTPRequest;

/**
 * An abstract superclass forKVP protocol binding extensions, which provides some
 * convenience methods to concrete implementations.
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class KVPProtocolExtension extends AbstractProtocolExtension {

    @Override
    public boolean canHandle(HTTPRequest request) {
        Boolean canHandle = request.getRequestString() != null && !request.getRequestString().startsWith("<");
        return canHandle;
    }
    
    protected String get(String key, Map<String, List<String>> m) {
        if (m.containsKey(key)) {
            return m.get(key).get(0);
        } else {
            return null;
        }
    }

    @Override
    public String getExtensionIdentifier() {
        return ExtensionsRegistry.KVP_IDENTIFIER;
    }
}
