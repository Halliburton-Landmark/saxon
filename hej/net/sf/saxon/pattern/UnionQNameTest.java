////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.tree.util.FastStringBuffer;

import java.util.List;

/**
 * A QNameTest that is the union of a number of supplied QNameTests
 */
public class UnionQNameTest implements QNameTest {

    List<QNameTest> tests;

    public UnionQNameTest(List<QNameTest> tests) {
        this.tests = tests;
    }

    /**
     * Test whether the QNameTest matches a given QName
     *
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        for (QNameTest test : tests) {
            if (test.matches(qname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The toString() method defines the format used in a package export, so it must be re-parseable
     * @return a string representation: the individual qname tests, separated by vertical bar
     */

    public String toString() {
        boolean started = false;
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C256);
        for (QNameTest qt : tests) {
            if (started) {
                fsb.append("|");
            } else {
                started = true;
            }
            fsb.append(qt.toString());
        }
        return fsb.toString();
    }
}

