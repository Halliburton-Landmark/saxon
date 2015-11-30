////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
 * Exception indicating that an attribute or namespace node has been written when
 * there is no open element to write it to
 */

public class NoOpenStartTagException extends XPathException {

    /**
     * Static factory method to create the exception
     *
     * @param nodeKind               the kind of node being created (attribute or namespace)
     * @param name                   the name of the node being created
     * @param hostLanguage           XSLT or XQuery (error codes are different in the two cases)
     * @param parentIsDocument       true if the nodes are being added to a document node (rather than an element)
     * @param isSerializing          true if the document is being created in the process of serialization
     * @param startElementLocationId integer that can be passed to the location provider to get the location
     *                               of the offending instruction that created the element node
     * @return the constructed exception object
     */

    public static NoOpenStartTagException makeNoOpenStartTagException(
        int nodeKind, String name, int hostLanguage, boolean parentIsDocument, boolean isSerializing,
            /*@Nullable*/  Location startElementLocationId) {
        String message;
        String errorCode;
        if (parentIsDocument) {
            if (isSerializing) {
                String kind = nodeKind == Type.ATTRIBUTE ? "attribute" : "namespace";
                message = "Cannot serialize a free-standing " + kind + " node (" + name + ')';
                errorCode = "SENR0001";
            } else {
                String kind = nodeKind == Type.ATTRIBUTE ? "an attribute" : "a namespace";
                message = "Cannot create " + kind + " node (" + name + ") whose parent is a document node";
                errorCode = hostLanguage == Configuration.XSLT ? "XTDE0420" : "XPTY0004";
            }
        } else {
            String kind = nodeKind == Type.ATTRIBUTE ? "An attribute" : "A namespace";
            message = kind + " node (" + name + ") cannot be created after a child of the containing element";
            errorCode = hostLanguage == Configuration.XSLT ? "XTDE0410" : "XQTY0024";
        }
        if (startElementLocationId != null) {
            message += ". Most recent element start tag was output at line " +
                startElementLocationId.getLineNumber() + " of module " +
                    StandardErrorListener.abbreviatePath(startElementLocationId.getSystemId());
        }
        NoOpenStartTagException err = new NoOpenStartTagException(message);
        err.setErrorCode(errorCode);
        return err;
    }

    public NoOpenStartTagException(String message) {
        super(message);
    }


}

