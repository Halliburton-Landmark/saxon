////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.flwor;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.OuterForExpressionCompiler;
import net.sf.saxon.evpull.EventIterator;
import net.sf.saxon.evpull.EventMappingFunction;
import net.sf.saxon.evpull.EventMappingIterator;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.IdentityWrapper;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.tree.iter.LookaheadIteratorImpl;
import net.sf.saxon.value.EmptySequence;

import java.util.Map;

/**
 * Expression class that implements the "outer for" clause of XQuery 3.0
 */
public class OuterForExpression extends ForExpression {

    /**
     * Get the cardinality of the range variable
     *
     * @return the cardinality of the range variable (StaticProperty.EXACTLY_ONE).
     *         in a subclass
     */

    protected int getRangeVariableCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    /**
     * Optimize the expression
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        // Very conservative optimization for the time being

        Expression sequence0 = getSequence();
        getSequenceOp().optimize(visitor, contextItemType);
        Expression action0 = getAction();
        getActionOp().optimize(visitor, contextItemType);

        if (sequence0 != getSequence() || action0 != getAction()) {
            // it's now worth re-attempting the "where" clause optimizations
            return optimize(visitor, contextItemType);
        }

        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        OuterForExpression forExp = new OuterForExpression();
        ExpressionTool.copyLocationInfo(this, forExp);
        forExp.setRequiredType(requiredType);
        forExp.setVariableQName(variableName);
        forExp.setSequence(getSequence().copy(rebindings));
        Expression newAction = getAction().copy(rebindings);
        forExp.setAction(newAction);
        forExp.variableName = variableName;
        ExpressionTool.rebindVariableReferences(newAction, this, forExp);
        return forExp;
    }

    /**
     * Iterate over the result of the expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        // Then create a MappingIterator which applies a mapping function to each
        // item in the base sequence. The mapping function is essentially the "return"
        // expression, wrapped in a MappingAction object that is responsible also for
        // setting the range variable at each step.

        SequenceIterator base = getSequence().iterate(context);
        LookaheadIterator ahead = LookaheadIteratorImpl.makeLookaheadIterator(base);
        if (ahead.hasNext()) {
            MappingFunction map = new MappingAction(context, getLocalSlotNumber(), getAction());
            return new MappingIterator(ahead, map);
        } else {
            context.setLocalVariable(getLocalSlotNumber(), EmptySequence.getInstance());
            return getAction().iterate(context);
        }
    }

    /**
     * Deliver the result of the expression as a sequence of events.
     *
     * @param context The dynamic evaluation context
     * @return the result of the expression as an iterator over a sequence of PullEvent objects
     * @throws XPathException if a dynamic error occurs during expression evaluation
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {

        // First create an iteration of the base sequence.

        // Then create an EventMappingIterator which applies a mapping function to each
        // item in the base sequence. The mapping function is essentially the "return"
        // expression, wrapped in an EventMappingAction object that is responsible also for
        // setting the range variable at each step.

        SequenceIterator base = getSequence().iterate(context);
        LookaheadIterator ahead = LookaheadIteratorImpl.makeLookaheadIterator(base);
        if (ahead.hasNext()) {
            EventMappingFunction map = new EventMappingAction(context, getLocalSlotNumber(), getAction());
            return new EventMappingIterator(ahead, map);
        } else {
            context.setLocalVariable(getLocalSlotNumber(), EmptySequence.getInstance());
            return getAction().iterateEvents(context);
        }
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator base = getSequence().iterate(context);
        int position = 1;
        int slot = getLocalSlotNumber();
        LookaheadIterator ahead = LookaheadIteratorImpl.makeLookaheadIterator(base);
        if (ahead.hasNext()) {
            while (true) {
                Item item = ahead.next();
                if (item == null) break;
                context.setLocalVariable(slot, item);
                getAction().process(context);
            }
        } else {
            context.setLocalVariable(getLocalSlotNumber(), EmptySequence.getInstance());
            getAction().process(context);
        }
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        SequenceIterator base = getSequence().iterate(context);
        int position = 1;
        int slot = getLocalSlotNumber();
        LookaheadIterator ahead = LookaheadIteratorImpl.makeLookaheadIterator(base);
        if (ahead.hasNext()) {
            while (true) {
                Item item = ahead.next();
                if (item == null) break;
                context.setLocalVariable(slot, item);
                getAction().evaluatePendingUpdates(context, pul);
            }
        } else {
            context.setLocalVariable(getLocalSlotNumber(), EmptySequence.getInstance());
            getAction().evaluatePendingUpdates(context, pul);
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the OuterFor expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new OuterForExpressionCompiler();
    }
//#endif


    protected void explainSpecializedAttributes(ExpressionPresenter out) {
        out.emitAttribute("outer", "true");
    }


}

//Copyright (c) Saxonica 2008. All rights reserved.


