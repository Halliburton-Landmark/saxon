////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

/**
 * Enumeration class giving the different streamability categories defined for stylesheet functions in XSLT 3.0
 */

public enum FunctionStreamability {
    UNCLASSIFIED("unclassified"),
    ABSORBING("absorbing"),
    INSPECTION("inspection"),
    FILTER("filter"),
    SHALLOW_DESCENT("shallow-descent"),
    DEEP_DESCENT("deep-descent"),
    ASCENT("ascent");

    public String streamabilityStr;

    FunctionStreamability(String v) {
        streamabilityStr = v;
    }

}
