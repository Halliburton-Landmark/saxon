////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.RetainedStaticContext;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.AtomicSortComparer;
import net.sf.saxon.expr.sort.EqualityComparer;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ErrorType;

import java.util.Properties;

/**
 * Abstract superclass for functions that take an optional collation argument, in which the
 * collation is not present as an explicit argument, either because it was defaulted in the
 * original function call, or because it has been bound during static analysis.
 */


public abstract class CollatingFunctionFixed extends SystemFunction {

    // The absolute collation URI
    private String collationName;

    // The collation corresponding to this collation name
    private StringCollator stringCollator = null;

    // The AtomicComparer to be used (not used by all collating functions)
    private AtomicComparer atomicComparer = null;




    /**
     * Get the collation if known statically, as a StringCollator object
     *
     * @return a StringCollator. Return null if the collation is not known statically.
     */

    public StringCollator getStringCollator() {
        return stringCollator;
    }

    @Override
    public void setRetainedStaticContext(RetainedStaticContext retainedStaticContext) {
        super.setRetainedStaticContext(retainedStaticContext);
        if (collationName == null) {
            collationName = retainedStaticContext.getDefaultCollationName();
            try {
                allocateCollator();
            } catch (XPathException e) {
                // ignore the failure, it will be reported later
            }
        }
    }

    public void setCollationName(String collationName) throws XPathException {
        this.collationName = collationName;
        allocateCollator();
    }

    private void allocateCollator() throws XPathException {
        stringCollator = getRetainedStaticContext().getConfiguration().getCollation(collationName);
        if (stringCollator == null) {
            throw new XPathException("Unknown collation " + collationName, "FOCH0002");
        }
    }

    /**
     * During static analysis, if types are known and the collation is known, pre-allocate a comparer
     * for comparing atomic values. Called by some collating functions during type-checking
     *
     * @param type0        the type of the first comparand
     * @param type1        the type of the second comparand
     * @param env          the static context
     * @param NaNequalsNaN true if two NaN values are to be considered equal
     */

    protected void preAllocateComparer(AtomicType type0, AtomicType type1, StaticContext env, boolean NaNequalsNaN) {
        StringCollator collation = getStringCollator();
        if (type0 == ErrorType.getInstance() || type1 == ErrorType.getInstance()) {
            // there will be no instances to compare, so we can use any comparer
            atomicComparer = EqualityComparer.getInstance();
            return;
        }

        if (NaNequalsNaN) {
            atomicComparer = AtomicSortComparer.makeSortComparer(
                    collation, type0.getPrimitiveType(), env.makeEarlyEvaluationContext());
        } else {
            atomicComparer = GenericAtomicComparer.makeAtomicComparer(
                    (BuiltInAtomicType) type0.getBuiltInBaseType(), (BuiltInAtomicType) type1.getBuiltInBaseType(),
                stringCollator, env.makeEarlyEvaluationContext());
        }
    }


    /**
     * Get the pre-allocated atomic comparer, if available
     *
     * @return the preallocated atomic comparer, or null
     */

    public AtomicComparer getPreAllocatedAtomicComparer() {
        return atomicComparer;
    }

    /**
     * During evaluation, get the pre-allocated atomic comparer if available, or allocate a new one otherwise
     * @param context the dynamic evaluation context
     * @return the pre-allocated comparer if one is available; otherwise, a newly allocated one, using the specified
     *         StringCollator for comparing strings
     */

    public AtomicComparer getAtomicComparer(XPathContext context) {
        if (atomicComparer != null) {
            return atomicComparer.provideContext(context);
        } else {
            return new GenericAtomicComparer(getStringCollator(), context);
        }
    }

    @Override
    public void exportAttributes(ExpressionPresenter out) {
        if (!collationName.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            out.emitAttribute("collation", collationName);
        }
    }

    @Override
    public void importAttributes(Properties attributes) throws XPathException {
        String collationName = attributes.getProperty("collation");
        if (collationName != null) {
            setCollationName(collationName);
        }
    }

}

