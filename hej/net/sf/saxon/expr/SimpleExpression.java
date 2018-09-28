////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.expr.oper.OperandArray;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.util.Arrays;


/**
 * An abstract implementation of Expression designed to make it easy to implement new expressions,
 * in particular, expressions to support extension instructions.
 * <p>An implementation of this class must supply the {@link Callable#call(XPathContext, net.sf.saxon.om.Sequence[])}
 * method to evaluate the expression and return its result.</p>
 */

public abstract class SimpleExpression extends Expression implements Callable {

    private OperandArray operanda;

    /**
     * Constructor
     */

    public SimpleExpression() {
    }

    /**
      * Set the data structure for the operands of this expression. This must be created during initialisation of the
      * expression and must not be subsequently changed
      * @param operanda the data structure for expression operands
      */

     protected void setOperanda(OperandArray operanda) {
         this.operanda = operanda;
     }

     /**
      * Get the data structure holding the operands of this expression.
      * @return the data structure holding expression operands
      */

     protected OperandArray getOperanda() {
         return operanda;
     }

     @Override
     public Iterable<Operand> operands() {
         return operanda.operands();
     }


    /**
     * Set the immediate sub-expressions of this expression.
     *
     * @param sub an array containing the sub-expressions of this expression
     */

    public void setArguments(Expression[] sub) {
        if (getOperanda() != null && getOperanda().getNumberOfOperands() > 0) {
            throw new IllegalArgumentException("Cannot replace existing argument array");
        }
        Expression[] sub2 = Arrays.copyOf(sub, sub.length);
        OperandRole[] roles = new OperandRole[sub.length];
        Arrays.fill(roles, OperandRole.NAVIGATE);
        setOperanda(new OperandArray(this, sub2, roles));
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {

        try {
            SimpleExpression se2 = getClass().newInstance();
            Expression[] a2 = new Expression[operanda.getNumberOfOperands()];
            int i = 0;
            for (Operand o : operands()) {
                a2[i++] = o.getChildExpression().copy(rebindings);
            }
            OperandArray o2 = new OperandArray(se2, a2, operanda.getRoles());
            se2.setOperanda(o2);
            return se2;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UnsupportedOperationException(getClass().getName() + ".copy()");
        }
    }

    /**
     * Helper method for subclasses to implement the copy() operation: this method can be applied
     * to the new expression to copy operands from the old expressions
     * @param se1 the expression being copied
     * @return the target object of this method (for convenience)
     */

    protected SimpleExpression copyOperandsFrom(SimpleExpression se1) {
        Expression[] a2 = new Expression[se1.operanda.getNumberOfOperands()];
        int i = 0;
        for (Operand o : se1.operands()) {
            a2[i++] = o.getChildExpression().copy(new RebindingMap());
        }
        OperandArray o2 = new OperandArray(this, a2, se1.operanda.getRoles());
        setOperanda(o2);
        return this;
    }

    /**
     * Determine the data type of the items returned by this expression. This implementation
     * returns "item()", which can be overridden in a subclass.
     *
     * @return the data type
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return Type.ITEM_TYPE;
    }

    /**
     * Determine the static cardinality of the expression. This implementation
     * returns "zero or more", which can be overridden in a subclass.
     */

    public int computeCardinality() {
        if ((getImplementationMethod() & Expression.EVALUATE_METHOD) == 0) {
            return StaticProperty.ALLOWS_ONE_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the node or atomic value that results from evaluating the
     *         expression; or null to indicate that the result is an empty
     *         sequence
     * @throws XPathException if any dynamic error occurs evaluating the
     *                        expression
     */

    public final Item evaluateItem(XPathContext context) throws XPathException {
        return call(context, evaluateArguments(context)).head();
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws XPathException if any dynamic error occurs evaluating the
     *                        expression
     */

    /*@NotNull*/
    public final SequenceIterator<? extends Item> iterate(XPathContext context) throws XPathException {
        return call(context, evaluateArguments(context)).iterate();
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public final void process(XPathContext context) throws XPathException {
        SequenceIterator<? extends Item> iter = call(context, evaluateArguments(context)).iterate();
        iter.forEachOrFail(
                it -> context.getReceiver().append(it, getLocation(), ReceiverOptions.ALL_NAMESPACES)
        );
    }

    /**
     * Internal method to evaluate the arguments prior to calling the generic call() method
     *
     * @param context the XPath dynamic context
     * @return the values of the (evaluated) arguments
     * @throws XPathException if a dynamic error occurs
     */

    private Sequence[] evaluateArguments(XPathContext context) throws XPathException {
        Sequence[] iters = new Sequence[getOperanda().getNumberOfOperands()];
        int i=0;
        for (Operand o : operands()) {
             iters[i++] = SequenceTool.toLazySequence(o.getChildExpression().iterate(context));
        }
        return iters;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter destination) throws XPathException {
        throw new XPathException("In general, stylesheets using extension instructions cannot be exported");
    }

    /**
     * Return a distinguishing name for the expression, for use in diagnostics.
     * By default the class name is used.
     *
     * @return a distinguishing name for the expression (defaults to the name of the implementation class)
     */

    public String getExpressionType() {
        return getClass().getName();
    }

}

