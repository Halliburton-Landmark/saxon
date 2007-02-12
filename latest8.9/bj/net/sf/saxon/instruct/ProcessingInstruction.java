package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An xsl:processing-instruction element in the stylesheet.
 */

public class ProcessingInstruction extends SimpleNodeConstructor {

    private Expression name;

    /**
     * Create an xsl:processing-instruction instruction
     * @param name the expression used to compute the name of the generated
     * processing-instruction
     */

    public ProcessingInstruction(Expression name) {
        this.name = name;
        adoptChildExpression(name);
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * @return the string "xsl:processing-instruction"
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_PROCESSING_INSTRUCTION;
    }

    /**
     * Get the expression that defines the processing instruction name
     * @return the expression that defines the processing instruction name
     */

    public Expression getNameExpression() {
        return name;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.PROCESSING_INSTRUCTION;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public Expression simplify(StaticContext env) throws XPathException {
        name = name.simplify(env);
        return super.simplify(env);
    }

    public void localTypeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        name = name.typeCheck(env, contextItemType);
        adoptChildExpression(name);

        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION, "processing-instruction/name", 0, null);
        role.setSourceLocator(this);
        name = TypeChecker.staticTypeCheck(name, SequenceType.SINGLE_STRING, false, role, env);
        adoptChildExpression(name);

        // Do early checking of name if known statically

        if (name instanceof Literal) {
            String s = ((Literal)name).getValue().getStringValue();
            checkName(s, env.makeEarlyEvaluationContext());
        }

        // Do early checking of content if known statically

        if (select instanceof Literal) {
            String s = ((Literal)select).getValue().getStringValue();
            String s2 = checkContent(s, env.makeEarlyEvaluationContext());
            if (!s2.equals(s)) {
                setSelect(new StringLiteral(s2), env.getConfiguration());
            }
        }
    }

    public int getDependencies() {
        return name.getDependencies() | super.getDependencies();
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
        list.add(name);
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (name == original) {
            name = replacement;
            found = true;
        }
        return found;
    }


    /**
     * Offer promotion for subexpressions. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception XPathException if any error is detected
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        name = doPromotion(name, offer);
        super.promoteInst(offer);
    }


    /**
     * Process this instruction, that is, produce a processing-instruction node in the
     * result sequence.
     * @param context the dynamic context of this transformation
     * @throws XPathException if any non-recoverable dynamic error occurs
     * @return always returns null in this implementation
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        String expandedName = evaluateName(context);
        if (expandedName != null) {
            String data = expandChildren(context).toString();
            data = checkContent(data, context);
            SequenceReceiver out = context.getReceiver();
            out.processingInstruction(expandedName, data, locationId, 0);
        }
        return null;
    }

    /**
     * Check the content of the node, and adjust it if necessary
     *
     * @param data the supplied content
     * @return the original content, unless adjustments are needed
     * @throws net.sf.saxon.trans.DynamicError
     *          if the content is invalid
     */

    protected String checkContent(String data, XPathContext context) throws DynamicError {
        int hh;
        while ((hh = data.indexOf("?>")) >= 0) {
            if (isXSLT()) {
                data = data.substring(0, hh + 1) + ' ' + data.substring(hh + 1);
            } else {
                DynamicError err = new DynamicError("Invalid characters (?>) in processing instruction", this);
                err.setErrorCode("XQDY0026");
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
                //context.getController().recoverableError(err);
            }
        }
        data = Whitespace.removeLeadingWhitespace(data).toString();
        return data;
    }

    public int evaluateNameCode(XPathContext context) throws XPathException {
        String expandedName = evaluateName(context);
        return context.getNamePool().allocate("", "", expandedName);
    }

    /**
     * Evaluate the name of the processing instruction. If it is invalid, report a recoverable error
     * and return null.
     * @param context
     * @return the name of the processing instruction (an NCName), or null, incicating an invalid name
     * @throws XPathException if evaluation fails, or if the recoverable error is treated as fatal
     */
    private String evaluateName(XPathContext context) throws XPathException {
        String expandedName = name.evaluateAsString(context).trim();
        checkName(expandedName, context);
        return expandedName;
    }

    private void checkName(String expandedName, XPathContext context) throws XPathException {
        if (!(context.getConfiguration().getNameChecker().isValidNCName(expandedName))) {
            DynamicError e = new DynamicError(
                    "Processing instruction name " + Err.wrap(expandedName) + " is not a valid NCName");
            e.setXPathContext(context);
            e.setErrorCode((isXSLT() ? "XTDE0890" : "XQDY0041"));
            throw dynamicError(this, e, context);
        }
        if (expandedName.equalsIgnoreCase("xml")) {
            DynamicError e = new DynamicError(
                    "Processing instructions cannot be named 'xml' in any combination of upper/lower case");
            e.setXPathContext(context);
            e.setErrorCode((isXSLT() ? "XTDE0890" : "XQDY0064"));
            throw dynamicError(this, e, context);
        }
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "processing-instruction");
        name.display(level + 1, out, config);
        super.display(level + 1, out, config);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
