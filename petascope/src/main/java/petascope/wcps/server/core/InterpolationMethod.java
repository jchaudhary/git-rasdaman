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
package petascope.wcps.server.core;


//A pair of an interpolation type and a null resistance. See the WCPS standard for an explanation of these.
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.WCPSException;
import petascope.util.WcpsConstants;

public class InterpolationMethod implements Cloneable {

    private String interpolationType;
    private String nullResistance;

    public InterpolationMethod(String interpolationType, String nullResistance) throws WCPSException
            {
        if ((interpolationType == null)
                || !(interpolationType.equals(WcpsConstants.MSG_NEAREST) || interpolationType.equals(WcpsConstants.MSG_LINEAR)
                || interpolationType.equals(WcpsConstants.MSG_CUBIC )
                || interpolationType.equals(WcpsConstants.MSG_QUADRATIC)
                || interpolationType.equals(WcpsConstants.MSG_NONE))) {
            throw new WCPSException(ExceptionCode.InvalidMetadata, "Invalid interpolation method: "
                    + interpolationType + " is not a legal interpolation type");
        }

        this.interpolationType = interpolationType;

        if ((nullResistance == null)
                || !(nullResistance.equals(WcpsConstants.MSG_FULL) || nullResistance.equals(WcpsConstants.MSG_NONE)
                || nullResistance.equals(WcpsConstants.MSG_HALF) || nullResistance.equals(WcpsConstants.MSG_OTHER))) {
            throw new WCPSException(ExceptionCode.InvalidMetadata, "Invalid interpolation method: "
                    + nullResistance + " is not a legal null resistance");
        }

        this.nullResistance = nullResistance;

    }

    public InterpolationMethod clone() {
        try {
            return new InterpolationMethod(interpolationType, nullResistance);
        } catch (WCPSException ime) {
            throw new RuntimeException("Invalid metadata while cloning InterpolationMethod. This is a software bug in WCPS.", ime);
        }

    }

    public boolean equals(InterpolationMethod im) {
        return interpolationType.equals(im.interpolationType)
                && nullResistance.equals(im.nullResistance);

    }

    public String getInterpolationType() {
        return interpolationType;

    }

    public String getNullResistance() {
        return nullResistance;

    }
}
