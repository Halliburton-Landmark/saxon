////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.functions.Current;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;

/**
 * Class for handling patterns with simple non-positional boolean predicates
 */
public class PatternWithPredicate extends Pattern {

    private Pattern basePattern;
    private Expression predicate;

    public PatternWithPredicate(Pattern basePattern, Expression predicate) {
        this.basePattern = basePattern;
        this.predicate = predicate;
    }

    /**
     * Replace any calls on current() by a variable reference bound to the supplied binding
     */
    @Override
    public void bindCurrent(Binding binding) {
        if (predicate instanceof Current) {
            predicate = new LocalVariableReference(binding);
        } else if (ExpressionTool.callsFunction(predicate, Current.FN_CURRENT)) {
            replaceCurrent(predicate, binding);
        }
    }

    /**
     * Allocate slots to any variables used within the pattern
     * @param slotManager holds details of the allocated slots
     * @param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(predicate, nextFree, slotManager);
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE. For patterns that
     * do not match nodes, return -1.
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    @Override
    public int getNodeKind() {
        return basePattern.getNodeKind();
    }

    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints,
     *         or it if matches atomic values
     */
    @Override
    public int getFingerprint() {
        return basePattern.getFingerprint();
    }

    /**
     * Determine whether this Pattern matches the given Node.
     * @param item    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */
    @Override
    public boolean matches(Item item, XPathContext context) throws XPathException {
        if (!basePattern.matches(item, context)) {
            return false;
        }
        XPathContext c2 = context.newMinorContext();
        SingletonIterator si = (SingletonIterator)SingletonIterator.makeIterator(item);
        si.next();
        c2.setCurrentIterator(si);
        try {
            return predicate.effectiveBooleanValue(c2);
        } catch (XPathException e) {
            if ("XTDE0640".equals(e.getErrorCodeLocalPart())) {
                // Treat circularity error as fatal (test error213)
                throw e;
            }
            XPathException err = new XPathException("An error occurred matching pattern {" + toString() + "}: ", e);
            err.setXPathContext(c2);
            err.setErrorCodeQName(e.getErrorCodeQName());
            err.setLocator(this);
            c2.getController().recoverableError(err);
            return false;
        }
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     *
     * @return an ItemType, as specific as possible, which all the matching items satisfy
     */
    /*@Nullable*/@Override
    public ItemType getItemType() {
        return basePattern.getItemType();
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */
    /*@NotNull*/
    @Override
    public Iterator<Expression> iterateSubExpressions() {
        return new PairIterator<Expression>(new PatternSponsor(basePattern), predicate);
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */
    @Override
    public Pattern analyze(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        ExpressionVisitor.ContextItemType cit = new ExpressionVisitor.ContextItemType(getItemType(), false);
        predicate = visitor.typeCheck(predicate, cit);
        return this;
    }

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     *
     * @return the default priority for the pattern
     */
    @Override
    public double getDefaultPriority() {
        return basePattern.getDefaultPriority() + 0.5;
    }

//#ifdefined STREAM
    @Override
    public boolean isMotionless(boolean allowExtensions) {
        return basePattern.isMotionless(allowExtensions) &&
                predicate.getStreamability(Expression.INSPECTION_CONTEXT, allowExtensions, null) == Expression.W3C_MOTIONLESS;
    }
//#endif

}

