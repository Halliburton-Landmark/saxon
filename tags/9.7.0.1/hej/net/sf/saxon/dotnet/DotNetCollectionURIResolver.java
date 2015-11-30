////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerable;
import cli.System.Collections.IEnumerator;
import cli.System.Uri;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.ResolveURI;
import net.sf.saxon.lib.StandardCollectionURIResolver;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AnyURIValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * This class implements the CollectionURIResolver interface by wrapping an IEnumerable which
 * returns Uri values (the URIs of the documents in the collection)
 */

public class DotNetCollectionURIResolver extends StandardCollectionURIResolver {

    private HashMap<String, IEnumerable> registeredCollections = new HashMap<String, IEnumerable>(20);

    public DotNetCollectionURIResolver() {
    }

    public void registerCollection(String uri, IEnumerable enumerable) {
        if (enumerable == null) {
            registeredCollections.remove(uri);
        } else if (uri == null) {
            registeredCollections.put("", enumerable);
        } else {
            registeredCollections.put(uri, enumerable);
        }
    }

    /**
     * Resolve a URI.
     *
     * @param href    The relative URI of the collection. This corresponds to the
     *                argument supplied to the collection() function. If the collection() function
     *                was called with no arguments (to get the "default collection") this argument
     *                will be null.
     * @param base    The base URI that should be used. This is the base URI of the
     *                static context in which the call to collection() was made, typically the URI
     *                of the stylesheet or query module
     * @param context The dynamic execution context
     * @return an Iterator over the documents in the collection. The items returned
     * by this iterator must be instances either of xs:anyURI, or of node() (specifically,
     * {@link net.sf.saxon.om.NodeInfo}.). If xs:anyURI values are returned, the corresponding
     * document will be retrieved as if by a call to the doc() function: this means that
     * the system first checks to see if the document is already loaded, and if not, calls
     * the registered URIResolver to dereference the URI. This is the recommended approach
     * to ensure that the resulting collection is stable: however, it has the consequence
     * that the documents will by default remain in memory for the duration of the query
     * or transformation.
     * <p/>
     * If the URI is not recognized, the method may either return an empty iterator,
     * in which case no error is reported, or it may throw an exception, in which case
     * the query or transformation fails. Returning null has the same effect as returning
     * an empty iterator.
     */

    public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {
        if (href == null) {
            IEnumerable ie = registeredCollections.get("");
            if (ie == null) {
                return super.resolve(href, base, context);
            }
            return new UriIterator(ie);
        }
        URI relativeURI;
        try {
            relativeURI = new URI(ResolveURI.escapeSpaces(href));
        } catch (URISyntaxException e) {
            XPathException err = new XPathException("Invalid relative URI " + Err.wrap(href, Err.VALUE) +
                                                            " passed to collection() function");
            err.setErrorCode("FODC0004");
            err.setXPathContext(context);
            throw err;
        }
        URI abs = makeAbsoluteURI(href, base, context, relativeURI);
        IEnumerable ie = registeredCollections.get(abs.toString());
        if (ie == null) {
            return super.resolve(href, base, context);
        }
        return new UriIterator(ie);
    }

    private static class UriIterator implements SequenceIterator {

        private IEnumerable enumerable;
        private IEnumerator enumerator;

        public UriIterator(IEnumerable enumerable) {
            this.enumerable = enumerable;
            this.enumerator = enumerable.GetEnumerator();
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         * properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         * and {@link #LOOKAHEAD}. It is always
         * acceptable to return the value zero, indicating that there are no known special properties.
         * It is acceptable for the properties of the iterator to change depending on its state.
         * @since 8.6
         */

        public int getProperties() {
            return 0;
        }

        /**
         * Get the next item in the sequence. This method changes the state of the
         * iterator, in particular it affects the result of subsequent calls of
         * position() and current().
         *
         * @return the next item, or null if there are no more items. Once a call
         * on next() has returned null, no further calls should be made. The preferred
         * action for an iterator if subsequent calls on next() are made is to return
         * null again, and all implementations within Saxon follow this rule.
         * @throws net.sf.saxon.trans.XPathException if an error occurs retrieving the next item
         * @since 8.4
         */

        /*@Nullable*/
        public Item next() throws XPathException {
            if (enumerator.MoveNext()) {
                Uri u = (Uri) enumerator.get_Current();
                return new AnyURIValue(u.ToString());
            } else {
                return null;
            }
        }

        public void close() {
        }

        /**
         * Get another SequenceIterator that iterates over the same items as the original,
         * but which is repositioned at the start of the sequence.
         * <p/>
         * This method allows access to all the items in the sequence without disturbing the
         * current position of the iterator. Internally, its main use is in evaluating the last()
         * function.
         * <p/>
         * This method does not change the state of the iterator.
         *
         * @return a SequenceIterator that iterates over the same items,
         * positioned before the first item
         * @throws net.sf.saxon.trans.XPathException if any error occurs
         * @since 8.4
         */

        /*@NotNull*/
        public SequenceIterator getAnother() throws XPathException {
            return new UriIterator(enumerable);
        }
    }
}

