////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon;

import net.sf.saxon.jaxp.SaxonTransformerFactory;


/**
 * A TransformerFactoryImpl instance can be used to create Transformer and Template
 * objects.
 * <p/>
 * <p>The system property that determines which Factory implementation
 * to create is named "javax.xml.transform.TransformerFactory". This
 * property names a concrete subclass of the TransformerFactory abstract
 * class. If the property is not defined, a platform default is be used.</p>
 * <p/>
 * <p>This implementation class implements the abstract methods on both the
 * javax.xml.transform.TransformerFactory and javax.xml.transform.sax.SAXTransformerFactory
 * classes.</p>
 * <p/>
 * <p>In Saxon 9.6, the JAXP transformation interface is re-implemented as a layer
 * on top of the s9api interface. This will affect applications that attempt to
 * down-cast from JAXP interfaces to the underlying implementation classes.</p>
 * <p/>
 * <p>This class is the "public" implementation of the TransformerFactory
 * interface for Saxon-HE. It is a trivial subclass of the internal class
 * {@link net.sf.saxon.jaxp.SaxonTransformerFactory}, which is in a separate package
 * along with the implementation classes to which it has protected access.</p>
 */

public class TransformerFactoryImpl extends SaxonTransformerFactory {

    public TransformerFactoryImpl() {
        super();
    }

    public TransformerFactoryImpl(Configuration config) {
        super(config);
    }

}

