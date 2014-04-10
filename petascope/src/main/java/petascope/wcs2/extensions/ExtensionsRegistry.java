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

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import petascope.HTTPRequest;
import petascope.wcs2.parsers.GetCoverageRequest;

/**
 * Protocol binding extensions are managed in this class.
 *
 * @author <a href="mailto:d.misev@jacobs-university.de">Dimitar Misev</a>
 */
public class ExtensionsRegistry {

    private static final Logger log = LoggerFactory.getLogger(ExtensionsRegistry.class);
    
    public static final String XML_IDENTIFIER = "http://www.opengis.net/spec/WCS_protocol-binding_post-xml/1.0";
    public static final String KVP_IDENTIFIER = "http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0/conf/get-kvp";
    public static final String REST_IDENTIFIER = "http://www.opengis.net/spec/WCS_protocol-binding_get-rest/1.0/conf/get-rest";    
    public static final String SOAP_IDENTIFIER = "http://www.opengis.net/spec/WCS_protocol-binding_soap/1.0";
    public static final String ENCODING_IDENTIFIER = "http://www.opengis.net/spec/GMLCOV/1.0/conf/gml-coverage";
    public static final String GEOTIFF_IDENTIFIER = "http://www.opengis.net/spec/WCS_coverage-encoding_geotiff/1.0/";
    public static final String JPEG2000_IDENTIFIER = "http://www.opengis.net/spec/WCS_coverage-encoding_jpeg2000/1.0/";
    public static final String NETCDF_IDENTIFIER = "http://www.opengis.net/spec/WCS_coverage-encoding_netcdf/1.0/";
    public static final String CRS_IDENTIFIER = "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs";
    public static final String RANGE_SUBSETTING_IDENTIFIER = "http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/";
    public static final String SCALING_IDENTIFIER = "http://www.opengis.net/spec/WCS_service-extension_scaling/1.0/conf/scaling";
    public static final String PROCESS_COVERAGE_IDENTIFIER = "http://www.opengis.net/spec/WCS_service-extension_processing/2.0/conf/processing";
    
    private static final Set<Extension> extensions = new HashSet<Extension>();
    private static final Set<String> extensionIds = new HashSet<String>();
    
    static {
        initialize();
    }

    /**
     * Initialize registry: load available protocol binding extensions
     */
    public static void initialize() {
        registerExtension(new XMLProtocolExtension());
        registerExtension(new SOAPProtocolExtension());
        registerExtension(new KVPProtocolExtension());
        registerExtension(new RESTProtocolExtension());        
        registerExtension(new GmlFormatExtension());
        registerExtension(new GeotiffFormatExtension());
        registerExtension(new JPEG2000FormatExtension());
        registerExtension(new MultipartGeotiffFormatExtension());
        registerExtension(new MultipartJPEG2000FormatExtension());
        registerExtension(new MultipartNetcdfFormatExtension());
        registerExtension(new RangeSubsettingExtension());
        registerExtension(new ScalingExtension());
        registerExtension(new ProcessCoverageExtension());
    }

    /**
     * Add new extension to the registry.
     * It will replace any other extension which has the same extension identifier.
     *
     * @param extension
     */
    public static void registerExtension(Extension extension) {
        log.info("Registered extension {}", extension);
        extensions.add(extension);
        extensionIds.add(extension.getExtensionIdentifier());
    }

    /**
     * @return a binding for the specified operation, that can parse the specified input, or null otherwise
     */
    public static ProtocolExtension getProtocolExtension(HTTPRequest request) {
        for (Extension extension : extensions) {
            if (extension instanceof ProtocolExtension && ((ProtocolExtension) extension).canHandle(request)) {
                return (ProtocolExtension) extension;
            }
        }
        return null;
    }
    
    public static FormatExtension getFormatExtension(GetCoverageRequest request) {
        for (Extension extension : extensions) {
            if (extension instanceof FormatExtension && ((FormatExtension) extension).canHandle(request)) {
                return (FormatExtension) extension;
            }
        }
        return null;
    }
    
    public static FormatExtension getFormatExtension(boolean multipart, String format) {
        return getFormatExtension(new GetCoverageRequest(null, format, multipart));
    }
    
    public static Extension getExtension(String extensionIdentifier) {
        for (Extension extension : extensions) {
            if (extension.getExtensionIdentifier().equals(extensionIdentifier)) {
                return extension;
            }
        }
        return null;
    }

    /**
     * @return the identifiers of all registered protocol binding extensions
     */
    public static String[] getExtensionIds() {
        return extensionIds.toArray(new String[extensionIds.size()]);
    }

    public static Set<Extension> getExtensions() {
        return extensions;
    }
}
