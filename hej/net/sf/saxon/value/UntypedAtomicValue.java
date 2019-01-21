////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.ComparisonException;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

/**
 * An Untyped Atomic value. This inherits from StringValue for implementation convenience, even
 * though an untypedAtomic value is not a String in the data model type hierarchy.
 */

public class UntypedAtomicValue extends StringValue {

    public static final UntypedAtomicValue ZERO_LENGTH_UNTYPED =
            new UntypedAtomicValue("");

    // If the value is used once as a number, it's likely that it will be used
    // repeatedly as a number, so we cache the result of conversion; similarly
    // for other types

    int cachedConversionType = -1;
    ConversionResult cachedConversionResult = null;

    /**
     * Constructor
     *
     * @param value the String value. Null is taken as equivalent to "".
     */

    public UntypedAtomicValue(/*@Nullable*/ CharSequence value) {
        this.value = value == null ? "" : value;
        typeLabel = BuiltInAtomicType.UNTYPED_ATOMIC;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    /*@NotNull*/
    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        UntypedAtomicValue v = new UntypedAtomicValue(value);
        v.cachedConversionResult = cachedConversionResult;
        v.cachedConversionType = cachedConversionType;
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    /*@NotNull*/
    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.UNTYPED_ATOMIC;
    }

    /**
     * Compare an untypedAtomic value with another value, using a given collator to perform
     * any string comparisons. This works by converting the untypedAtomic value to the type
     * of the other operand, which is the correct behavior for operators like "=" and "!=",
     * but not for "eq" and "ne": in the latter case, the untypedAtomic value is converted
     * to a string and this method is therefore not used.
     *
     * @param other    the value to compare this value with
     * @param collator the collation to be used for comparing strings
     * @param context  the XPath dynamic context
     * @return -1 if the this value is less than the other, 0 if they are equal, +1 if this
     *         value is greater.
     * @throws ClassCastException if the value cannot be cast to the type of the other operand
     */

    public int compareTo(/*@NotNull*/ AtomicValue other, /*@NotNull*/ StringCollator collator, /*@NotNull*/ XPathContext context) {
        if (other instanceof NumericValue) {
            DoubleValue doubleValue = null;
            if (cachedConversionType == StandardNames.XS_DOUBLE) {
                try {
                    doubleValue = (DoubleValue) cachedConversionResult.asAtomic();
                } catch (ValidationException e) {
                    throw new NumberFormatException(e.getMessage());
                }
            }
            if (doubleValue == null) {
                try {
                    doubleValue = new DoubleValue(context.getConfiguration().getConversionRules()
                                                          .getStringToDoubleConverter().stringToNumber(value));
                } catch (NumberFormatException e) {
                    XPathException xe = new XPathException("Cannot convert untyped atomic value '" + getStringValue()
                                                                   + "' to a double ", "FORG0001");
                    throw new ComparisonException(xe);
                }
                cachedConversionType = StandardNames.XS_DOUBLE;
                cachedConversionResult = doubleValue;
            }
            return doubleValue.compareTo((NumericValue)other);
        } else if (other instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                // This optimization avoids creating String objects for the purpose of the comparison
                return CodepointCollator.compareCS(getStringValueCS(), other.getStringValueCS());
            } else {
                return collator.compareStrings(getStringValue(), other.getStringValue());
            }
        } else {
            final Configuration config = context.getConfiguration();
            Converter converter = config.getConversionRules().getConverter(BuiltInAtomicType.UNTYPED_ATOMIC, other.getItemType());
            ConversionResult result = converter.convert(this);
            if (result instanceof ValidationFailure) {
                XPathException xe = new XPathException("Cannot convert untyped atomic value '" + getStringValue()
                                                               + "' to type " + other.getItemType(), "FORG0001");
                throw new ComparisonException(xe);
            }
            return ((Comparable) result).compareTo(other);

        }
    }

    /**
     * Ask whether there is a known match key for comparing this value as
     * a particular type. Note this is not used for string comparisons
     */

    public synchronized ConversionResult getConversionResultIfKnown(int atomicType) {
        return cachedConversionType == atomicType ? cachedConversionResult : null;
    }

    /**
     * Compute and cache a conversion result converting this value to a particular type
     * <p>This method should not be used if the converter is namespace-sensitive, since the cached
     * value may then be incorrect.</p>
     *
     * @param atomicType the fingerprint of the type to which the value is being convertered
     * @param converter the converter used to convert to the target type
     */

    public synchronized ConversionResult obtainConversionResult(int atomicType, StringConverter converter) {
        // The logic here doesn't take into account the possibility that the cached result was obtained
        // using a different converter. The risk is small, but
        ConversionResult knownResult = getConversionResultIfKnown(atomicType);
        if (knownResult == null) {
            cachedConversionType = atomicType;
            return cachedConversionResult = converter.convertString(value);
        } else {
            return knownResult;
        }
    }

    /**
     * Cache the result of a conversion
     */

    public synchronized void setConversionResult(int atomicType, AtomicValue value) {
        cachedConversionType = atomicType;
        cachedConversionResult = value;
    }

    /**
     * For displaying the value in error messages, prefix with "u" to highlight that it's
     * untyped atomic
     * @return a short depiction of the string for use in error messages
     */

    public String toShortString() {
        return "u" + super.toShortString();
    }


}

