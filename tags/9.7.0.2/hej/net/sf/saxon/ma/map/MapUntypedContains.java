////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.UntypedAtomicValue;

/**
 * Implementation of an internal function map:untyped-contains(Map, key) => boolean,
 * which is like map:contains except that if the supplied key is untyped atomic, it
 * is converted to all the possible types present in the map and returns true if the
 * key after conversion is present. In addition, if the supplied key is NaN then the
 * result is always false.
 */
public class MapUntypedContains extends ExtensionFunctionDefinition {

    private final static StructuredQName name =
            new StructuredQName("map", NamespaceConstant.MAP_FUNCTIONS, "untyped-contains");
    private final static SequenceType[] ARG_TYPES =
            new SequenceType[]{HashTrieMap.SINGLE_MAP_TYPE, SequenceType.SINGLE_ATOMIC};

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the function name
     */

    public StructuredQName getFunctionQName() {
        return name;
    }

    /**
     * Get the minimum number of arguments required by the function
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 2;
    }

    /**
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @return the required types of the arguments, as defined by the function signature. Normally
     *         this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     *         that allow a variable number of arguments, the array can be smaller than this, with the last
     *         entry in the array providing the required type for all the remaining arguments.
     */

    public SequenceType[] getArgumentTypes() {
        return ARG_TYPES;
    }

    /**
     * Get the type of the result of the function
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @param suppliedArgumentTypes the static types of the supplied arguments to the function.
     *                              This is provided so that a more precise result type can be returned in the common
     *                              case where the type of the result depends on the types of the arguments.
     * @return the return type of the function, as defined by its function signature
     */

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_BOOLEAN;
    }

    /**
     * Ask whether the result actually returned by the function can be trusted,
     * or whether it should be checked against the declared type.
     *
     * @return true if the function implementation warrants that the value it returns will
     *         be an instance of the declared result type. The default value is false, in which case
     *         the result will be checked at run-time to ensure that it conforms to the declared type.
     *         If the value true is returned, but the function returns a value of the wrong type, the
     *         consequences are unpredictable.
     */

    public boolean trustResultType() {
        return true;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    /*@Nullable*/
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                ConversionRules rules = context.getConfiguration().getConversionRules();
                MapItem map = (MapItem) arguments[0].head();
                AtomicValue key = (AtomicValue) arguments[1].head();
                if (key instanceof UntypedAtomicValue) {
                    for (PrimitiveUType prim : map.getKeyUType().decompose()) {
                        BuiltInAtomicType t = (BuiltInAtomicType)prim.toItemType();
                        StringConverter converter = t.getStringConverter(rules);
                        ConversionResult av = converter.convert(key);
                        if (av instanceof ValidationFailure) {
                            // ignore it for now
                        } else if (map.get(av.asAtomic()) != null) {
                            return BooleanValue.TRUE;
                        }
                    }
                    return BooleanValue.FALSE;
                }
                boolean result = map.get(key) != null;
                return BooleanValue.get(result);
            }
        };
    }
}

// Copyright (c) 2015 Saxonica Limited. All rights reserved.

