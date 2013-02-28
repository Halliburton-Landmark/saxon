package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Value;


/**
* Handler for xsl:if elements in stylesheet. <br>
* The xsl:if element has a mandatory attribute test, a boolean expression.
* The content is output if the test condition is true.
*/

public class XSLIf extends StyleElement {

    /*@Nullable*/ private Expression test;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        test = prepareTestAttribute(this);
        if (test==null) {
            reportAbsence("test");
        }
    }

    /**
     * Process all the attributes, for an element where the only permitted attribute is "test"
     * @param se the containing element
     * @return the expression represented by the test attribute, or null if the attribute is absent
     * @throws XPathException if an error is encountered
     */

    public static Expression prepareTestAttribute(StyleElement se) throws XPathException {

        String testAtt=null;

		AttributeCollection atts = se.getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.TEST)) {
        		testAtt = atts.getValue(a);
        	} else {
        		se.checkUnknownAttribute(atts.getNodeName(a));
        	}
        }

        if (testAtt==null) {
            return null;
        } else {
            try  {
                return se.makeExpression(testAtt);
            } catch (XPathException err) {
                // already reported: prevent further errors
                return new Literal(BooleanValue.FALSE);
            }
        }
    }

    public void validate(Declaration decl) throws XPathException {
        test = typeCheck("test", test);
    }

    /**
    * Mark tail-recursive calls on stylesheet functions. For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (test instanceof Literal) {
            Value testVal = ((Literal)test).getValue();
            // condition known statically, so we only need compile the code if true.
            // This can happen with expressions such as test="function-available('abc')".
            try {
                if (testVal.effectiveBooleanValue()) {
                    return compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
//                    Block block = new Block();
//                    block.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
//                    compileChildren(exec, block, true);
//                    return block.simplify(getStaticContext());
                } else {
                    return null;
                }
            } catch (XPathException err) {
                // fall through to non-optimizing case
            }
        }

        Expression action = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        if (action == null) {
            return null;
        }
        Expression[] conditions = {test};
        Expression[] actions = {action};

        return new Choose(conditions, actions);
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