package net.sf.saxon.functions;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.GroupIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;


/**
* Implements the XSLT function current-grouping-key()
*/

public class CurrentGroupingKey extends SystemFunction {

    /**
     * Determine the dependencies
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        GroupIterator gi = c.getCurrentGroupIterator();
        if (gi==null) {
            return null;
        }
        return gi.getCurrentGroupingKey();
     }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//