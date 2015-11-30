////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.tree.util.Navigator;

/**
 * A Location corresponding to an attribute in a document (often a stylesheet)
 */

public class AttributeLocation implements Location {

    String systemId;
    int lineNumber;
    int columnNumber;
    StructuredQName elementName;
    StructuredQName attributeName;

    public AttributeLocation(NodeInfo element, StructuredQName attributeName) {
        this.systemId = element.getSystemId();
        this.lineNumber = element.getLineNumber();
        this.columnNumber = element.getColumnNumber();
        this.elementName = Navigator.getNodeName(element);
        this.attributeName = attributeName;
    }

    public AttributeLocation(StructuredQName elementName, StructuredQName attributeName, Location location) {
        this.systemId = location.getSystemId();
        this.lineNumber = location.getLineNumber();
        this.columnNumber = location.getColumnNumber();
        this.elementName = elementName;
        this.attributeName = attributeName;
    }

    /**
     * Get the name of the containing element
     *
     * @return the name of the containing element in the stylesheet
     */

    public StructuredQName getElementName() {
        return elementName;
    }

    /**
     * Get the name of the containing attribute
     *
     * @return the name of the containing attribute in the stylesheet. May be null
     * if the XPath expression is contained in a text node
     */

    public StructuredQName getAttributeName() {
        return attributeName;
    }

    /**
     * Get the column number. This column number is relative to the line identified by the line number.
     * Column numbers start at 1.
     *
     * @return the column number, or -1 if the information is not available.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Get the system ID. This should be the system identifier of an XML external entity; if a stylesheet module
     * comprises multiple external entities, the distinction should be retained. This means, for example, that
     * an instruction in a stylesheet can have a different system identifier from its parent instruction. However,
     * SAX parsers only provide location information at the element level, so when part of an XPath expression
     * is in a different external entity from other parts of the same expression, this distinction is lost.
     * <p/>
     * <p>The system identifier of a node is in general not the same as its base URI. The base URI is affected
     * by xml:base attributes; the system identifier is not.</p>
     *
     * @return the system ID, or null if the information is not available.
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the Public ID
     *
     * @return usually null
     */
    public String getPublicId() {
        return null;
    }

    /**
     * Get the line number. This line number is relative to the external entity identified by the system identifier.
     * Line numbers start at 1. The value may be an approximation; SAX parsers only provide line number information
     * at the level of element nodes.
     *
     * @return the line number, or -1 if the information is not available.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get an immutable copy of this Location object. By default Location objects may be mutable, so they
     * should not be saved for later use. The result of this operation holds the same location information,
     * but in an immutable form.
     */
    public Location saveLocation() {
        return this;
    }
}

