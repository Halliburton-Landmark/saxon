package net.sf.saxon.expr.instruct;

import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.trace.InstructionInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This object represents the compiled form of a user-written function, template, attribute-set, etc
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 *
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public abstract class Procedure implements Serializable, Container, InstructionInfo, LocationProvider {

    protected Expression body;
    private Executable executable;
    private String systemId;
    private int lineNumber;
    private SlotManager stackFrameMap;
    private int hostLanguage;

    public Procedure() {}

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    public void setBody(Expression body) {
        this.body = body;
        body.setContainer(this);
    }

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    public int getHostLanguage() {
        return hostLanguage;
    }

    public final Expression getBody() {
        return body;
    }

    public void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    public final Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return this;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getColumnNumber() {
        return -1;
    }

    /*@Nullable*/ public String getPublicId() {
        return null;
    }

    public String getSystemId(long locationId) {
        return systemId;
    }

    public int getLineNumber(long locationId) {
        return lineNumber;
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
    }

    public Object getProperty(String name) {
        return null;
    }


    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     */

    public Iterator<String> getProperties() {
        final List<String> list = Collections.emptyList();
        return list.iterator();
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