////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExistsCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.ExistsAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.value.BooleanValue;


/**
 * Implementation of the fn:exists function
 */
public class Exists extends Aggregate {

    // TODO: not clear why the code here is so different from Empty().


    public Expression makeOptimizedFunctionCall(
            ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, final Expression... arguments) throws XPathException {

        // See if we can deduce the answer from the cardinality
        int c = arguments[0].getCardinality();
        if (c == StaticProperty.ALLOWS_ONE_OR_MORE) {
            return Literal.makeLiteral(BooleanValue.TRUE);
        } else if (c == StaticProperty.ALLOWS_ZERO) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        // Don't sort the argument
        arguments[0] = arguments[0].unordered(false, visitor.isOptimizeForStreaming());

        // Rewrite
        //    exists(A|B) => exists(A) or exists(B)
        if (arguments[0] instanceof VennExpression && !visitor.isOptimizeForStreaming()) {
            VennExpression v = (VennExpression) arguments[0];
            if (v.getOperator() == Token.UNION) {
                Expression e0 = SystemFunction.makeCall("exists", getRetainedStaticContext(), v.getLhsExpression());
                Expression e1 = SystemFunction.makeCall("exists", getRetainedStaticContext(), v.getRhsExpression());
                return new OrExpression(e0, e1).optimize(visitor, contextInfo);
            }
        }
        return null;
    }



    private static boolean exists(SequenceIterator iter) throws XPathException {
        boolean result;
        if ((iter.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            result = ((LookaheadIterator) iter).hasNext();
        } else {
            result = iter.next() != null;
        }
        iter.close();
        return result;
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        return BooleanValue.get(exists(arguments[0].iterate()));
    }


    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Exists expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ExistsCompiler();
    }
//#endif

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    //@Override
    public ExistsAdjunct getStreamingAdjunct() {
        return new ExistsAdjunct();
    }

    //#endif
}

