package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.value.BooleanValue;


public class Lang extends SystemFunction implements CallableExpression {

 
    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (argument.length==1) {
            if (contextItemType == null) {
                XPathException err = new XPathException("The context item for lang() is undefined");
                err.setErrorCode("XPDY0002");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            } else if (contextItemType instanceof AtomicType) {
                XPathException err = new XPathException("The context item for lang() is not a node");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo target;
        if (argument.length > 1) {
            target = (NodeInfo)argument[1].evaluateItem(c);
        } else {
            target = getAndCheckContextItem(c);
        }
        final Item arg0Val = argument[0].evaluateItem(c);
        final String testLang = (arg0Val==null ? "" : arg0Val.getStringValue());
        boolean b = isLang(testLang, target);
        return BooleanValue.get(b);
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return (argument.length == 1 ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0);
    }

    /**
    * Test whether the context node has the given language attribute
    * @param arglang the language being tested
    * @param target the target node
    */

    public static boolean isLang(String arglang, NodeInfo target) {
        String doclang = null;
        NodeInfo node = target;

        while(node!=null) {
            doclang = node.getAttributeValue(StandardNames.XML_LANG);
            if (doclang!=null) {
                break;
            }
            node = node.getParent();
            if (node==null) {
                return false;
            }
        }

        if (doclang==null) {
            return false;
        }

        while (true) {
            if (arglang.equalsIgnoreCase(doclang)) {
                return true;
            }
            int hyphen = doclang.indexOf("-");
            if (hyphen<0) {
                return false;
            }
            doclang = doclang.substring(0, hyphen);
        }
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        NodeInfo target;
        if (arguments.length > 1) {
            target = (NodeInfo)arguments[1].next();
        } else {
            target = getAndCheckContextItem(context);
        }
        final Item arg0Val = arguments[0].next();
        final String testLang = (arg0Val==null ? "" : arg0Val.getStringValue());
        boolean b = isLang(testLang, target);
        return SingletonIterator.makeIterator(BooleanValue.get(b));
    }

    /**
     * Get the context item, checking that it exists and is a node
     * @param context the XPath dynamic context
     * @return the context node
     * @throws XPathException if there is no context item or if the context item is not a node
     */

    private NodeInfo getAndCheckContextItem(XPathContext context) throws XPathException {
        NodeInfo target;
        Item current = context.getContextItem();
        if (current==null) {
            XPathException err = new XPathException("The context item for lang() is undefined");
            err.setErrorCode("XPDY0002");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        if (!(current instanceof NodeInfo)) {
            XPathException err = new XPathException("The context item for lang() is not a node");
            err.setErrorCode("XPTY0004");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        target = (NodeInfo)current;
        return target;
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