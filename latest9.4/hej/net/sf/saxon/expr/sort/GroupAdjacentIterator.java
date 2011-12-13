package net.sf.saxon.expr.sort;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * A GroupAdjacentIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-adjacent="x". The groups are returned in
 * order of first appearance.
 * <p>
 * Each step of this iterator advances to the first item of the next group,
 * leaving the members of that group in a saved list.
 */

public class GroupAdjacentIterator implements GroupIterator, LookaheadIterator {

    private SequenceIterator population;
    private Expression keyExpression;
    private StringCollator collator;
    private AtomicComparer comparer;
    private ComparisonKey currentComparisonKey;
    private XPathContext baseContext;
    private XPathContext runningContext;
    /*@Nullable*/ private AtomicValue currentKey = null;
    private List currentMembers;
    private AtomicValue nextKey = null;
    private Item next;
    private Item current = null;
    private int position = 0;

    public GroupAdjacentIterator(SequenceIterator population, Expression keyExpression,
                                 XPathContext baseContext, StringCollator collator)
    throws XPathException {
        this.population = population;
        this.keyExpression = keyExpression;
        this.baseContext = baseContext;
        this.runningContext = baseContext.newMinorContext();
        runningContext.setCurrentIterator(population);
        this.collator = collator;
        int type = keyExpression.getItemType(baseContext.getConfiguration().getTypeHierarchy()).getPrimitiveType();
        this.comparer = AtomicSortComparer.makeSortComparer(collator, type, baseContext);
        next = population.next();
        if (next != null) {
            nextKey = (AtomicValue)keyExpression.evaluateItem(runningContext);
        }
    }

    private void advance() throws XPathException {
        currentMembers = new ArrayList(20);
        currentMembers.add(current);
        while (true) {
            Item nextCandidate = population.next();
            if (nextCandidate == null) {
                break;
            }
            AtomicValue candidateKey =
                    (AtomicValue)keyExpression.evaluateItem(runningContext);
            try {
                if (currentComparisonKey.equals(comparer.getComparisonKey(candidateKey))) {
                    currentMembers.add(nextCandidate);
                } else {
                    next = nextCandidate;
                    nextKey = candidateKey;
                    return;
                }
            } catch (ClassCastException e) {
                XPathException err = new XPathException("Grouping key values are of non-comparable types (" +
                        Type.displayTypeName(currentKey) +
                        " and " +
                        Type.displayTypeName(candidateKey) + ')');
                err.setIsTypeError(true);
                err.setXPathContext(runningContext);
                throw err;
            }
        }
        next = null;
        nextKey = null;
    }

    public Value getCurrentGroupingKey() {
        return currentKey;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public boolean hasNext() {
        return next != null;
    }

    public Item next() throws XPathException {
        if (next == null) {
            current = null;
            position = -1;
            return null;
        }
        current = next;
        currentKey = nextKey;
        currentComparisonKey = comparer.getComparisonKey(currentKey);
        position++;
        advance();
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        population.close();
    }

    /*@NotNull*/
    public SequenceIterator getAnother() throws XPathException {
        return new GroupAdjacentIterator(population.getAnother(), keyExpression, baseContext, collator);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LOOKAHEAD;
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