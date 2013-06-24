package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.trans.XPathException;

/**
* Implement the XPath 2.0 default-collation() function
*/

public class DefaultCollation extends CompileTimeFunction {

    /**
    * Pre-evaluate the function
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        String s = visitor.getStaticContext().getDefaultCollationName();
        return new StringLiteral(s);
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