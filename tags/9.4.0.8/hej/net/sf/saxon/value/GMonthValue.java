package net.sf.saxon.value;

import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ConversionResult;
import net.sf.saxon.type.ValidationFailure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the xs:gMonth data type
 */

public class GMonthValue extends GDateValue {

    private static Pattern regex =
            //Pattern.compile("--([0-9][0-9])(--)?(Z|[+-][0-9][0-9]:[0-9][0-9])?");
            Pattern.compile("--([0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");
            // The commented-out pattern tolerates the bogus format --MM-- which was wrongly permitted by the original schema spec

    private GMonthValue(){}

    public static ConversionResult makeGMonthValue(CharSequence value, ConversionRules rules) {
        GMonthValue g = new GMonthValue();
        Matcher m = regex.matcher(Whitespace.trimWhitespace(value));
        if (!m.matches()) {
            return new ValidationFailure("Cannot convert '" + value + "' to a gMonth");
        }
        String base = m.group(1);
        String tz = m.group(2);
        String date = "2000-" + base + "-01" + (tz==null ? "" : tz);
        g.typeLabel = BuiltInAtomicType.G_MONTH;
        return setLexicalValue(g, date, rules);
    }

    public GMonthValue(byte month, int tz) {
        this(month, tz, BuiltInAtomicType.G_MONTH);
    }

    public GMonthValue(byte month, int tz, AtomicType type) {
        this.year = 2000;
        this.month = month;
        this.day = 1;
        setTimezoneInMinutes(tz);
        this.typeLabel = type;
    }

     /**
     * Make a copy of this date, time, or dateTime value
      * @param typeLabel
      */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        GMonthValue v = new GMonthValue(month, getTimezoneInMinutes());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.G_MONTH;
    }

    /*@NotNull*/ public CharSequence getPrimitiveStringValue() {

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.TINY);

        sb.append("--");
        appendTwoDigits(sb, month);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    /**
     * Add a duration to this date/time value
     *
     * @param duration the duration to be added (which might be negative)
     * @return a new date/time value representing the result of adding the duration. The original
     *         object is not modified.
     * @throws net.sf.saxon.trans.XPathException
     *
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        XPathException err = new XPathException("Cannot add a duration to an xs:gMonth");
        err.setErrorCode("XPTY0004");
        throw err;
    }

    /**
     * Return a new date, time, or dateTime with the same normalized value, but
     * in a different timezone
     *
     * @param tz the new timezone, in minutes
     * @return the date/time in the new timezone
     */

    public CalendarValue adjustTimezone(int tz) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(tz);
        return new GMonthValue(dt.getMonth(), dt.getTimezoneInMinutes());
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