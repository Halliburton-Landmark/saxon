////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Whitespace;

/**
 * This class supports the XSLT element-available function.  Note that when running in a 2.0 processor,
 * it only looks for XSLT 2.0 instructions; but when running in a 3.0 processor, it recognizes all
 * elements in the XSLT namespace whether or not they are classified as instructions.
 */

public class ElementAvailable extends SystemFunction {

    public static boolean isXslt20Instruction(int fp) {
        switch (fp) {
            case StandardNames.XSL_ANALYZE_STRING:
            case StandardNames.XSL_APPLY_IMPORTS:
            case StandardNames.XSL_APPLY_TEMPLATES:
            case StandardNames.XSL_ATTRIBUTE:
            case StandardNames.XSL_CALL_TEMPLATE:
            case StandardNames.XSL_CHOOSE:
            case StandardNames.XSL_COMMENT:
            case StandardNames.XSL_COPY:
            case StandardNames.XSL_COPY_OF:
            case StandardNames.XSL_DOCUMENT:
            case StandardNames.XSL_ELEMENT:
            case StandardNames.XSL_FALLBACK:
            case StandardNames.XSL_FOR_EACH:
            case StandardNames.XSL_FOR_EACH_GROUP:
            case StandardNames.XSL_IF:
            case StandardNames.XSL_MESSAGE:
            case StandardNames.XSL_NAMESPACE:
            case StandardNames.XSL_NEXT_MATCH:
            case StandardNames.XSL_PERFORM_SORT:
            case StandardNames.XSL_PROCESSING_INSTRUCTION:
            case StandardNames.XSL_RESULT_DOCUMENT:
            case StandardNames.XSL_SEQUENCE:
            case StandardNames.XSL_TEXT:
            case StandardNames.XSL_VALUE_OF:
            case StandardNames.XSL_VARIABLE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isXslt30Element(int fp) {
        switch (fp) {
            case StandardNames.XSL_ACCEPT:
            case StandardNames.XSL_ACCUMULATOR:
            case StandardNames.XSL_ACCUMULATOR_RULE:
            case StandardNames.XSL_ANALYZE_STRING:
            case StandardNames.XSL_APPLY_IMPORTS:
            case StandardNames.XSL_APPLY_TEMPLATES:
            case StandardNames.XSL_ASSERT:
            case StandardNames.XSL_ATTRIBUTE:
            case StandardNames.XSL_ATTRIBUTE_SET:
            case StandardNames.XSL_BREAK:
            case StandardNames.XSL_CALL_TEMPLATE:
            case StandardNames.XSL_CATCH:
            case StandardNames.XSL_CHARACTER_MAP:
            case StandardNames.XSL_CHOOSE:
            case StandardNames.XSL_COMMENT:
            case StandardNames.XSL_CONTEXT_ITEM:
            case StandardNames.XSL_COPY:
            case StandardNames.XSL_COPY_OF:
            case StandardNames.XSL_DECIMAL_FORMAT:
            case StandardNames.XSL_DOCUMENT:
            case StandardNames.XSL_ELEMENT:
            case StandardNames.XSL_EVALUATE:
            case StandardNames.XSL_EXPOSE:
            case StandardNames.XSL_FALLBACK:
            case StandardNames.XSL_FOR_EACH:
            case StandardNames.XSL_FOR_EACH_GROUP:
            case StandardNames.XSL_FORK:
            case StandardNames.XSL_FUNCTION:
            case StandardNames.XSL_GLOBAL_CONTEXT_ITEM:
            case StandardNames.XSL_IF:
            case StandardNames.XSL_IMPORT:
            case StandardNames.XSL_IMPORT_SCHEMA:
            case StandardNames.XSL_INCLUDE:
            case StandardNames.XSL_ITERATE:
            case StandardNames.XSL_KEY:
            case StandardNames.XSL_MAP:
            case StandardNames.XSL_MAP_ENTRY:
            case StandardNames.XSL_MATCHING_SUBSTRING:
            case StandardNames.XSL_MERGE:
            case StandardNames.XSL_MERGE_ACTION:
            case StandardNames.XSL_MERGE_KEY:
            case StandardNames.XSL_MERGE_SOURCE:
            case StandardNames.XSL_MESSAGE:
            case StandardNames.XSL_MODE:
            case StandardNames.XSL_NAMESPACE:
            case StandardNames.XSL_NAMESPACE_ALIAS:
            case StandardNames.XSL_NEXT_ITERATION:
            case StandardNames.XSL_NEXT_MATCH:
            case StandardNames.XSL_NON_MATCHING_SUBSTRING:
            case StandardNames.XSL_NUMBER:
            case StandardNames.XSL_ON_COMPLETION:
            case StandardNames.XSL_ON_EMPTY:
            case StandardNames.XSL_ON_NON_EMPTY:
            case StandardNames.XSL_OTHERWISE:
            case StandardNames.XSL_OUTPUT:
            case StandardNames.XSL_OUTPUT_CHARACTER:
            case StandardNames.XSL_OVERRIDE:
            case StandardNames.XSL_PACKAGE:
            case StandardNames.XSL_PARAM:
            case StandardNames.XSL_PERFORM_SORT:
            case StandardNames.XSL_PRESERVE_SPACE:
            case StandardNames.XSL_PROCESSING_INSTRUCTION:
            case StandardNames.XSL_RESULT_DOCUMENT:
            case StandardNames.XSL_SEQUENCE:
            case StandardNames.XSL_SORT:
            case StandardNames.XSL_STREAM:
            case StandardNames.XSL_STRIP_SPACE:
            case StandardNames.XSL_STYLESHEET:
            case StandardNames.XSL_TEMPLATE:
            case StandardNames.XSL_TEXT:
            case StandardNames.XSL_TRANSFORM:
            case StandardNames.XSL_TRY:
            case StandardNames.XSL_USE_PACKAGE:
            case StandardNames.XSL_VALUE_OF:
            case StandardNames.XSL_VARIABLE:
            case StandardNames.XSL_WHEN:
            case StandardNames.XSL_WITH_PARAM:
            case StandardNames.XSL_WHERE_POPULATED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Determine at run-time whether a particular instruction is available. Returns true
     * for XSLT instructions, Saxon extension instructions, and registered
     * user-defined extension instructions. If the processor is an XSLT 3.0 processor,
     * all XSLT *elements* are recognized; an XSLT 2.0 processor recognizes only
     * XSLT 2.0 elements that are classified as *instructions*.
     *
     * @param lexicalName the lexical QName of the element
     * @param context     the XPath evaluation context
     * @return true if the instruction is available, in the sense of the XSLT element-available() function
     * @throws XPathException if a dynamic error occurs (e.g., a bad QName)
     */

    private boolean isElementAvailable(String lexicalName, XPathContext context) throws XPathException {

        StructuredQName qName;
        boolean is30 = getRetainedStaticContext().getXPathVersion() >= 30;
        try {
            if (lexicalName.indexOf(':') < 0) {
                CharSequence local = Whitespace.trimWhitespace(lexicalName);
                if (!NameChecker.isValidNCName(local)) {
                    throw new XPathException("Invalid EQName passed to element-available(): local part is not a valid NCName");
                }
                String uri = getRetainedStaticContext().getURIForPrefix("", true);
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                qName = StructuredQName.fromLexicalQName(lexicalName, false, is30, getRetainedStaticContext());
            }
        } catch (XPathException e) {
            e.setErrorCode("XTDE1440");
            e.setXPathContext(context);
            throw e;
        }

        if (qName.hasURI(NamespaceConstant.XSLT)) {
            int fp = context.getConfiguration().getNamePool().getFingerprint(NamespaceConstant.XSLT, qName.getLocalPart());
            return is30 ? isXslt30Element(fp) : isXslt20Instruction(fp);
        } else {
            return context.getConfiguration().isExtensionElementAvailable(qName);
        }
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
        String lexicalQName = arguments[0].head().getStringValue();
        boolean b = isElementAvailable(lexicalQName, context);
        return BooleanValue.get(b);
    }
}
