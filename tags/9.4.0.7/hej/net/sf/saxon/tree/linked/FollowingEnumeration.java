package net.sf.saxon.tree.linked;

import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;

final class FollowingEnumeration extends TreeEnumeration {

    private NodeImpl root;

    public FollowingEnumeration(NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);
        root = (DocumentImpl)node.getDocumentRoot();
        // skip the descendant nodes if any
        int type = node.getNodeKind();
        if (type==Type.ATTRIBUTE || type==Type.NAMESPACE) {
            next = ((NodeImpl)node.getParent()).getNextInDocument(root);
        } else {
            do {
                next = (NodeImpl)node.getNextSibling();
                if (next==null) node = (NodeImpl)node.getParent();
            } while (next==null && node!=null);
        }
        while (!conforms(next)) {
            step();
        }
    }

    protected void step() {
        next = next.getNextInDocument(root);
    }

    /**
    * Get another enumeration of the same nodes
    */

    /*@NotNull*/ public AxisIterator getAnother() {
        return new FollowingEnumeration(start, nodeTest);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//