////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ComplexType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.Whitespace;

import java.util.Arrays;

/**
 * The IgnorableWhitespaceStripper removes whitespace text nodes belonging to elements
 * whose schema-defined type defines element-only content
 *
 * @author Michael H. Kay
 */


public class IgnorableWhitespaceStripper extends ProxyReceiver {

    // We implement our own stack to avoid the overhead of allocating objects.

    private boolean[] stripStack = new boolean[100];
    private int top = 0;

    public IgnorableWhitespaceStripper(Receiver next) {
        super(next);
    }

    public void startElement(NodeName nameCode, SchemaType type, Location location, int properties) throws XPathException {
        //System.err.println("startElement " + getNamePool().getDisplayName(nameCode));
        nextReceiver.startElement(nameCode, type, location, properties);

        boolean strip = false;
        if (type != Untyped.getInstance()) {
            // if the element has element-only content, whitespace stripping is enabled
            if (type.isComplexType() &&
                    !((ComplexType) type).isSimpleContent() &&
                    !((ComplexType) type).isMixedContent()) {
                strip = true;
            }
        }
        //System.err.println("strip = " + strip);

        // put "strip" value on top of stack

        top++;
        if (top >= stripStack.length) {
            stripStack = Arrays.copyOf(stripStack, top*2);
        }
        stripStack[top] = strip;
    }

    /**
     * Handle an end-of-element event
     */

    public void endElement() throws XPathException {
        nextReceiver.endElement();
        top--;
    }

    /**
     * Handle a text node
     */

    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        if (chars.length() > 0 && (!stripStack[top] || !Whitespace.isWhite(chars))) {
            nextReceiver.characters(chars, locationId, properties);
        }
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     *
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return true;
    }
}

// Copyright (c) Saxonica 2005 - 2008.