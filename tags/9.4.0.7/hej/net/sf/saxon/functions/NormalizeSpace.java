package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Whitespace;

/**
 * Implement the XPath normalize-space() function
 */

public class NormalizeSpace extends SystemFunction {

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        int d = super.getIntrinsicDependencies();
        if (argument.length == 0) {
            d |= StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
        }
        return d;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            XPathException err = new XPathException("The context item for normalize-space() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Pre-evaluate a function at compile time. Functions that do not allow
    * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral((Value)evaluateItem(
                    visitor.getStaticContext().makeEarlyEvaluationContext()));
        }
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        if (argument.length == 0) {
            Item item = c.getContextItem();
            if (item == null) {
                dynamicError("Context item for normalize-space() is absent", "XPDY0002", c);
                return null;
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(item.getStringValueCS()));
        } else {
            AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
            if (sv==null) {
                return StringValue.EMPTY_STRING;
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(sv.getStringValueCS()));
        }
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * <p>This method is implemented for normalize-space() because it is quite often used in a
     * boolean context to test whether a value exists and is non-white, and because testing for the
     * presence of non-white characters is a lot more efficient than constructing the normalized
     * string, especially because of early-exit.</p>
     *
     * @param c The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the expression
     */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        CharSequence cs;
        if (argument.length == 0) {
            Item item = c.getContextItem();
            if (item == null) {
                dynamicError("Context item for normalize-space() is absent", "XPDY0002", c);
                return false;
            }
            cs = item.getStringValueCS();
        } else {
            AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
            if (sv==null) {
                return false;
            }
            cs = sv.getStringValueCS();
        }
        return !Whitespace.isWhite(cs);
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