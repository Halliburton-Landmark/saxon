////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.*;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.NoDynamicContextException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the extension function array:sort(array, function) => array
 */
public class ArraySort extends SystemFunction {

    private static class MemberToBeSorted{
        public Sequence value;
        public GroundedValue sortKey;
        int originalPosition;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     *
     * @return an expression representing a call of this extension function
     */
    @Override
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        ArrayItem array = (ArrayItem) arguments[0].head();
        final List<MemberToBeSorted> inputList = new ArrayList<MemberToBeSorted>(array.arrayLength());
        int i = 0;
        StringCollator collation;
        if (arguments.length == 1) {
            collation = context.getConfiguration().getCollation(getRetainedStaticContext().getDefaultCollationName());
        } else {
            StringValue collName = (StringValue)arguments[1].head();
            if (collName == null) {
                collation = context.getConfiguration().getCollation(getRetainedStaticContext().getDefaultCollationName());
            } else {
                collation = context.getConfiguration().getCollation(collName.getStringValue(), getStaticBaseUriString());
            }
        }
        Function key = null;
        if (arguments.length == 3){
            key = (Function) arguments[2].head();
        }
        for (Sequence seq: array){
            MemberToBeSorted member = new MemberToBeSorted();
            member.value = seq;
            member.originalPosition = i++;
            if (key != null) {
                member.sortKey = SequenceTool.toGroundedValue(dynamicCall(key, context, new Sequence[]{seq}));
            } else {
                member.sortKey = atomize(seq);
            }
            inputList.add(member);
        }
        final AtomicComparer atomicComparer =  AtomicSortComparer.makeSortComparer(
                collation, StandardNames.XS_ANY_ATOMIC_TYPE, context);
        Sortable sortable = new Sortable() {
            public int compare(int a, int b) {
                int result = compareSortKeys(inputList.get(a).sortKey, inputList.get(b).sortKey, atomicComparer);
                if (result == 0){
                    return inputList.get(a).originalPosition - inputList.get(b).originalPosition;
                } else {
                    return result;
                }
            }

            public void swap(int a, int b) {
                MemberToBeSorted temp = inputList.get(a);
                inputList.set(a, inputList.get(b));
                inputList.set(b, temp);
            }
        };
        try {
            GenericSorter.quickSort(0, array.arrayLength(), sortable);
        } catch (ClassCastException e) {
            XPathException err = new XPathException("Non-comparable types found while sorting: " + e.getMessage());
            err.setErrorCode("XPTY0004");
            throw err;
        }
        List<Sequence> outputList = new ArrayList<Sequence>(array.arrayLength());
        for (MemberToBeSorted member: inputList){
            outputList.add(member.value);
        }
        return new SimpleArrayItem(outputList);
    }

    public static int compareSortKeys(GroundedValue a, GroundedValue b, AtomicComparer comparer) {
        UnfailingIterator iteratora = a.iterate();
        UnfailingIterator iteratorb = b.iterate();
        while (true){
            AtomicValue firsta = (AtomicValue) iteratora.next();
            AtomicValue firstb = (AtomicValue) iteratorb.next();
            if (firsta == null){
                if (firstb == null){
                    return 0;
                }
                else {
                    return -1;
                }
            }
            else if (firstb == null){
                return +1;
            }
            else {
                try {
                    int first = comparer.compareAtomicValues(firsta, firstb);
                    if (first == 0){
                        continue;
                    } else {
                        return first;
                    }
                } catch (NoDynamicContextException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static GroundedValue atomize(Sequence input) throws XPathException {
        try {
            SequenceIterator iterator = input.iterate();
            SequenceIterator mapper = Atomizer.getAtomizingIterator(iterator, false);
            return SequenceExtent.makeSequenceExtent(mapper);
        } catch (XPathException e) {
            throw new XPathException(e);
        }
    }
}
