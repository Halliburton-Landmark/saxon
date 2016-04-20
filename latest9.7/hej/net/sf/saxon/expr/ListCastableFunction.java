////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
 * A function item representing a constructor function for a list type
 */

public class ListCastableFunction extends ListConstructorFunction {

    /**
     * Create the constructor function.
     *
     * @param targetType the type to which the function will convert its input
     * @param resolver   namespace resolver for use if the target type is namespace-sensitive
     */

    public ListCastableFunction(ListType targetType, NamespaceResolver resolver, boolean allowEmpty) throws MissingComponentException {
        super(targetType, resolver, allowEmpty);
    }

    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */
    public FunctionItemType getFunctionItemType() {
        AtomicType resultType = BuiltInAtomicType.ANY_ATOMIC;
        if (getMemberType().isAtomicType()) {
            resultType = (AtomicType) getMemberType();
        }

        return new SpecificFunctionType(
                new SequenceType[]{SequenceType.ANY_SEQUENCE},
                SequenceType.makeSequenceType(resultType, StaticProperty.ALLOWS_ZERO_OR_MORE));
    }

    /**
     * Get the name of the function, or null if it is anonymous
     *
     * @return the function name, or null for an anonymous inline function
     */
    public StructuredQName getFunctionName() {
        return null;
    }

    /**
     * Invoke the function
     *
     * @param context the XPath dynamic evaluation context
     * @param args    the actual arguments to be supplied
     * @return the result of invoking the function
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs within the function
     */
    public BooleanValue call(XPathContext context, Sequence[] args) throws XPathException {
        AtomicValue val = (AtomicValue) args[0].head();
        if (val == null) {
            return BooleanValue.get(allowEmpty);
        }
        if (!(val instanceof StringValue) || val instanceof AnyURIValue) {
            return BooleanValue.FALSE;
        }

        ConversionRules rules = context.getConfiguration().getConversionRules();
        CharSequence cs = val.getStringValueCS();
        ValidationFailure failure = targetType.validateContent(cs, nsResolver, rules);
        return BooleanValue.get(failure == null);
    }
}

