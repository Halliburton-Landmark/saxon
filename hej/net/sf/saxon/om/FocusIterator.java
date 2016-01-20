////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.trans.XPathException;

/**
 * An iterator that maintains the values of position() and current(),
 * typically implemented as a wrapper over an iterator which does not maintain these values itself.
 */
public interface FocusIterator extends SequenceIterator {

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */
    public Item current();

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p/>
     * This method does not change the state of the iterator.
     *
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */
    public int position();

    /**
     * Get the position of the last item in the sequence
     * @return the position of the last item
     * @throws XPathException if a failure occurs reading the sequence
     */

    public int getLength() throws XPathException;

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p/>
     * <p>This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.</p>
     * <p/>
     * <p>This method does not change the state of the iterator.</p>
     * <p/>
     * <p>Some implementations of this method may regenerate the input sequence, creating
     * new nodes with different identity from the original. This is not recommended, but is
     * hard to prevent. This causes no problem for the primary usage of this method to support
     * the last() function, but it has been known to cause trouble in other situations.</p>
     *
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs
     * @since 8.4
     */

    FocusIterator getAnother() throws XPathException;
}