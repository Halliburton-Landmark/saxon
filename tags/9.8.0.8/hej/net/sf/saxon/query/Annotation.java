////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.query;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an annotation that appears in a function or variable declarations
 */
public class Annotation {

    public static final StructuredQName UPDATING = new StructuredQName("", NamespaceConstant.XQUERY, "updating");
    public static final StructuredQName SIMPLE = new StructuredQName("", NamespaceConstant.XQUERY, "simple");
    public static final StructuredQName PRIVATE = new StructuredQName("", NamespaceConstant.XQUERY, "private");
    public static final StructuredQName PUBLIC = new StructuredQName("", NamespaceConstant.XQUERY, "public");


    // The name of the annotation
    private StructuredQName qName = null;

    // The list of paramters (all strings or numbers) associated with the annotation
    private List<AtomicValue> annotationParameters = null;

    /**
     * Create an annotation
     *
     * @param name the annotation name (a QName)
     */

    public Annotation(StructuredQName name) {
        this.qName = name;
    }

    /**
     * Get the name of the annotation (a QName)
     *
     * @return the annotation name
     */

    public StructuredQName getAnnotationQName() {
        return qName;
    }

    /**
     * Add a value to the list of annotation parameters
     *
     * @param value the value to be added. This will always be a string or number,
     *              but Saxon enforces this only at the level of the query parser
     */

    public void addAnnotationParameter(AtomicValue value) {
        if (annotationParameters == null) {
            annotationParameters = new ArrayList<AtomicValue>();
        }
        annotationParameters.add(value);
    }

    /**
     * Get the list of annotation parameters
     *
     * @return the list of parameters
     */

    public List<AtomicValue> getAnnotationParameters() {
        if (annotationParameters == null) {
            annotationParameters = new ArrayList<AtomicValue>();
        }
        return annotationParameters;
    }

    public boolean equals(Object other) {
        return other instanceof Annotation &&
                qName.equals(((Annotation)other).qName) &&
                annotationParameters.equals(((Annotation)other).annotationParameters);
    }

    public int hashCode() {
        return qName.hashCode() ^ annotationParameters.hashCode();
    }


}

