////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.ValueOf;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;


/**
 * An xsl:value-of element in the stylesheet. <br>
 * The xsl:value-of element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".
 * This must be a valid String expression</li>
 * <li>an optional disable-output-escaping attribute, value "yes" or "no"</li>
 * <li>an optional separator attribute</li>
 * </ul>
 */

public final class XSLValueOf extends XSLLeafNodeConstructor {

    private boolean disable = false;
    /*@Nullable*/ private Expression separator;


    public void prepareAttributes() throws XPathException {

        String selectAtt = null;
        String disableAtt = null;
        String separatorAtt = null;

        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals("disable-output-escaping")) {
                disableAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("select")) {
                selectAtt = atts.getValue(a);
                select = makeExpression(selectAtt, a);
            } else if (f.equals("separator")) {
                separatorAtt = atts.getValue(a);
                separator = makeAttributeValueTemplate(separatorAtt, a);
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (disableAtt != null) {
            disable = processBooleanAttribute("disable-output-escaping", disableAtt);
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        super.validate(decl);
        select = typeCheck("select", select);
        separator = typeCheck("separator", separator);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0870";
    }

    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        Configuration config = getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        if (separator == null && select != null && xPath10ModeIsEnabled()) {
            // Handle XSLT 1.0 backwards compatibility
            select = config.getTypeChecker(true).processValueOf(select, config);
        } else {
            if (separator == null) {
                if (select == null) {
                    separator = new StringLiteral(StringValue.EMPTY_STRING);
                } else {
                    separator = new StringLiteral(StringValue.SINGLE_SPACE);
                }
            }
        }
        ValueOf inst = new ValueOf(select, disable, false);
        inst.setRetainedStaticContext(makeRetainedStaticContext());
        compileContent(exec, decl, inst, separator);
        return inst;
    }

}

