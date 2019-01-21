////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.NumericValue;

/**
 * The XPath 2.0 remove() function
 */

public class Remove extends SystemFunction {

    @Override
    public Expression makeFunctionCall(Expression[] arguments) {

        if (Literal.isAtomic(arguments[1])) {
            Sequence<?> index = ((Literal) arguments[1]).getValue();
            if (index instanceof IntegerValue) {
                try {
                    long value = ((IntegerValue) index).longValue();
                    if (value <= 0) {
                        return arguments[0];
                    } else if (value == 1) {
                        return new TailExpression(arguments[0], 2);
                    }
                } catch (XPathException err) {
                    //
                }
            }
        }

        return super.makeFunctionCall(arguments);
    }

    /**
     * Evaluate the expression as a general function call
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence<?> call(XPathContext context, Sequence[] arguments) throws XPathException {
        NumericValue n = (NumericValue) arguments[1].head();
        int pos = (int) n.longValue();
        if (pos < 1) {
            return arguments[0];
        }
        return SequenceTool.toLazySequence2(new RemoveIterator<>(arguments[0].iterate(), pos));
    }

    /**
     * An implementation of SequenceIterator that returns all items except the one
     * at a specified position.
     */

    public static class RemoveIterator<T extends Item<?>> implements SequenceIterator<T>, LastPositionFinder {

        SequenceIterator<T> base;
        int removePosition;
        int basePosition = 0;
        T current = null;

        public RemoveIterator(SequenceIterator<T> base, int removePosition) {
            this.base = base;
            this.removePosition = removePosition;
        }

        public T next() throws XPathException {
            current = base.next();
            basePosition++;
            if (current != null && basePosition == removePosition) {
                current = base.next();
                basePosition++;
            }
            return current;
        }

        public void close() {
            base.close();
        }

        /**
         * Get the last position (that is, the number of items in the sequence). This method is
         * non-destructive: it does not change the state of the iterator.
         * The result is undefined if the next() method of the iterator has already returned null.
         */

        public int getLength() throws XPathException {
            if (base instanceof LastPositionFinder) {
                int x = ((LastPositionFinder) base).getLength();
                if (removePosition >= 1 && removePosition <= x) {
                    return x - 1;
                } else {
                    return x;
                }
            } else {
                // This shouldn't happen, because this iterator only has the LAST_POSITION_FINDER property
                // if the base iterator has the LAST_POSITION_FINDER property
                throw new AssertionError("base of removeIterator is not a LastPositionFinder");
            }
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         * properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
         * and {@link SequenceIterator#LOOKAHEAD}. It is always
         * acceptable to return the value zero, indicating that there are no known special properties.
         * It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return base.getProperties() & LAST_POSITION_FINDER;
        }
    }

    public String getStreamerName() {
        return "Remove";
    }

}

