////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.linked;

import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;

/**
* This axis cannot be requested directly in an XPath expression
* but is used when evaluating xsl:number. It is provided because
* taking the union of the two axes would be very inefficient
*/

final class PrecedingOrAncestorEnumeration extends TreeEnumeration {


    public PrecedingOrAncestorEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        advance();   
    }

    protected void step() {
        next = next.getPreviousInDocument();
    }

    /**
    * Get another iterator over the same nodes
    */
    
    /*@NotNull*/ public AxisIterator getAnother() {
        return new PrecedingOrAncestorEnumeration(start, nodeTest);
    }

}

