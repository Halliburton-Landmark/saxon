package net.sf.saxon.lib;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;


/**
 * A ModuleURIResolver is used when resolving references to
 * query modules. It takes as input a URI that identifies the module to be loaded, and a set of
 * location hints, and returns one or more StreamSource obects containing the queries
 * to be imported.
* @author Michael H. Kay
*/

public interface ModuleURIResolver extends Serializable {

    /**
     * Locate a query module, or a set of query modules, given the identifying URI and
     * a set of associated location hints.
     *
     * <p>The module URI resolver is always invoked when locating library modules. It is also
     * invoked when loading the main query module in cases where this is provided in the form
     * of a URI: specifically, when running a query from the command line specifying the -u
     * option, or with a supplied query file name that starts with "http:" or "file:". When
     * locating the main query module, the moduleURI and baseURI parameters will be null, and
     * the resolver must either return null (which delegates to the standard ModuleURIResolver),
     * or must return a singleton StreamSource, that is, an array of length 1.</p>
     * 
     * @param moduleURI the module URI of the module to be imported; or null when
     * loading a non-library module.
     * @param baseURI The base URI of the module containing the "import module" declaration;
     * null if no base URI is known
     * @param locations The set of URIs specified in the "at" clause of "import module",
     * which serve as location hints for the module
     * @return an array of {@link StreamSource} objects each identifying the contents of a query module to be
     * imported. Each StreamSource must contain a
     * non-null absolute System ID which will be used as the base URI of the imported module,
     * and either an {@link java.io.InputStream} or an {@link java.io.Reader} representing the text of the module.
     * <p>The contained InputStream or Reader must be positioned at the start of the
     * content to be read; it will be consumed by the system and will be closed after use.</p>
     * <p>The method may alternatively return null, in which case the system attempts to resolve the URI using the
     * standard module URI resolver.</p>
     * @throws XPathException if the module cannot be located, and if delegation to the default
     * module resolver is not required.
    */

    public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException;

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
