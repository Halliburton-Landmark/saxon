package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.functions.regex.JRegularExpression;
import net.sf.saxon.functions.regex.RegularExpression;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.util.regex.PatternSyntaxException;


/**
* This class implements the replace() function for replacing
* substrings that match a regular expression
*/

public class Replace extends SystemFunction implements CallableExpression {

    /*@Nullable*/ private RegularExpression regexp;
    private boolean allow30features = false;
    private boolean replacementChecked = false;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     /*@NotNull*/
     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        allow30features = DecimalValue.THREE.equals(visitor.getStaticContext().getXPathLanguageLevel());
        Expression e = simplifyArguments(visitor);
        if (e == this) {
            maybePrecompile(visitor);
        }
        return e;
    }

    private void maybePrecompile(ExpressionVisitor visitor) throws XPathException {
        // compile the regular expression once if possible
        if (regexp == null) {
            try {
                regexp = Matches.tryToCompile(argument, 1, 3, visitor.getStaticContext());
            } catch (XPathException err) {
                err.setLocator(this);
                throw err;
            }

            // check that it's not a pattern that matches ""
            if (regexp != null && regexp.matches("")) {
                XPathException err = new XPathException("The regular expression in replace() must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                err.setLocator(this);
                throw err;
            }
        }
    }


    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        // try once again to compile the regular expression once if possible
        // (used when the regex has been identified as a constant as a result of earlier rewrites)
        if (e == this) {
            maybePrecompile(visitor);
        }
        if (argument[2] instanceof StringLiteral) {
            // Do early checking of the replacement expression if known statically
            String rep = ((StringLiteral)argument[2]).getStringValue();
            String msg = checkReplacement(rep);
            replacementChecked = true;
            if (msg != null) {
                XPathException ex = new XPathException(msg, "FORX0004");
                ex.setLocator(this);
                return new ErrorExpression(ex);
            }
        }
        return e;
    }

    /**
     * Get the compiled regular expression if available, otherwise return null
     * @return the compiled regex, or null
     */

    public RegularExpression getCompiledRegularExpression() {
        return regexp;
    }    

    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return eval((StringValue)argument[0].evaluateItem(c),
                (StringValue)argument[1].evaluateItem(c),
                (StringValue)argument[2].evaluateItem(c),
                (argument.length==3 ? null : (StringValue)argument[3].evaluateItem(c)), c);
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
        Item result = eval((StringValue)arguments[0].next(), (StringValue)arguments[1].next(), (StringValue)arguments[2].next(),
                (arguments.length == 3 ? null : (StringValue)arguments[3].next()), context);
        return Value.asIterator(result);
    }

    /**
     * Internal method that does the work
     * @param inputArg
     * @param regexArg
     * @param replaceArg
     * @param flagsArg
     * @param context
     * @return
     * @throws XPathException
     */

    private Item eval(StringValue inputArg, StringValue regexArg,
                      StringValue replaceArg, StringValue flagsArg, XPathContext context) throws XPathException {    

        if (inputArg==null) {
            inputArg = StringValue.EMPTY_STRING;
        }

        CharSequence replacement = replaceArg.getStringValueCS();
        if (!replacementChecked) {
            // if it is a string literal, the check was done at compile time
            String msg = checkReplacement(replacement);
            if (msg != null) {
                dynamicError(msg, "FORX0004", context);
            }
        }

        RegularExpression re = regexp;
        if (re == null) {

            CharSequence flags;

            if (flagsArg == null) {
                flags = "";
            } else {
                flags = flagsArg.getStringValueCS();
            }

            try {
                int flagBits = JRegularExpression.setFlags(flags);
                int options = RegularExpression.XPATH20;
                if (context.getConfiguration().getXMLVersion() == Configuration.XML11) {
                    options |= RegularExpression.XML11;
                }
                if (context.getConfiguration().getXsdVersion() == Configuration.XSD11) {
                    options |= RegularExpression.XSD11;
                }
                if (allow30features) {
                    options |= RegularExpression.XPATH30;
                }
                re = new JRegularExpression(regexArg.getStringValueCS(), options, flagBits, null);

            } catch (XPathException err) {
                XPathException de = new XPathException(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(context);
                de.setLocator(this);
                throw de;
            } catch (PatternSyntaxException err) {
                XPathException de = new XPathException(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(context);
                de.setLocator(this);
                throw de;
            }

            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                dynamicError(
                        "The regular expression in replace() must not be one that matches a zero-length string",
                        "FORX0003", context);
            }
        }
        String input = inputArg.getStringValue();
        CharSequence res = re.replace(input, replacement);
        return StringValue.makeStringValue(res);
    }

    /**
     * Check the contents of the replacement string
     * @param rep the replacement string
     * @return null if the string is OK, or an error message if not
    */

    public static String checkReplacement(CharSequence rep) {
        for (int i=0; i<rep.length(); i++) {
            char c = rep.charAt(i);
            if (c == '$') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next < '0' || next > '9') {
                        return "Invalid replacement string in replace(): $ sign must be followed by digit 0-9";
                    }
                } else {
                    return "Invalid replacement string in replace(): $ sign at end of string";
                }
            } else if (c == '\\') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next != '\\' && next != '$') {
                        return "Invalid replacement string in replace(): \\ character must be followed by \\ or $";
                    }
                } else {
                    return "Invalid replacement string in replace(): \\ character at end of string";
                }
            }
        }
        return null;
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