////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.SpecificFunctionType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.z.IntPredicate;

/**
 * Default handler class for accepting the result from parsing JSON strings
 */
public class JsonHandler {

    public boolean escape;
    protected IntPredicate charChecker;
    private XPathContext context;

    private Function fallbackFunction = null;
    private static final String REPLACEMENT = "\ufffd";

    public void setContext(XPathContext context) {
        this.context = context;
    }

    public XPathContext getContext() {
        return context;
    }

    public Sequence getResult() throws XPathException {
        return null;
    };

    /**
     * Set the key to be written for the next entry in an object/map
     *
     * @param key the key for the entry (null implies no key)
     * @param isEscaped true if backslashes within the key are to be treated as signalling
     *                  a JSON escape sequence
     * @return true if the key is already present in the map, false if it is not
     */
    public boolean setKey(String key, boolean isEscaped) throws XPathException {
        return false;
    };

    /**
     * Open a new array
     *
     * @throws XPathException if any error occurs
     */
    public void startArray() throws XPathException {};

    /**
     * Close the current array
     *
     * @throws XPathException if any error occurs
     */
    public void endArray() throws XPathException {};

    /**
     * Start a new object/map
     *
     * @throws XPathException if any error occurs
     */
    public void startMap() throws XPathException {};

    /**
     * Close the current object/map
     *
     * @throws XPathException if any error occurs
     */
    public void endMap() throws XPathException {};

    /**
     * Write a numeric value
     *
     * @param asString the string representation of the value
     * @param asDouble the double representation of the value
     * @throws XPathException if any error occurs
     */
    public void writeNumeric(String asString, double asDouble) throws XPathException {};

    /**
     * Write a string value
     *
     * @param val The string to be written (which may or may not contain JSON escape sequences, according to the
     * options that were set)
     * @param isEscaped set to true if backslash is to be recognized as an escape character. This does not necessarily
     *                  mean that all special characters are already escaped.
     * @throws XPathException if any error occurs
     */
    public void writeString(String val, boolean isEscaped) throws XPathException {};

    /**
     * Optionally apply escaping or unescaping to a value.
     * @param val the string to be escaped or unEscaped
     * @param isKey true if this string is a map key
     * @param isEscaped true if a backslash in the existing string is to be interpreted as an escape
     * @param requireEscaped true if the output is required to be escaped. In this case, any escape sequences
     *                       already present in the input will be retained, and any "special" characters
     *                       in the input that are not already escaped will become escaped. If false, existing
     *                       escape sequences in the input will be unescaped, and characters that are invalid
     *                       in XML will be rejected.
     * @return the escaped or unescaped string
     * @throws XPathException
     */

    public String reEscape(String val, boolean isKey, boolean isEscaped, boolean requireEscaped) throws XPathException {
        CharSequence escaped;
        if (requireEscaped) {
            escaped = JsonReceiver.escape(val, isEscaped, true, new IntPredicate() {
                public boolean matches(int value) {
                    return (value >= 0 && value <= 0x1F) ||
                        (value >= 0x7F && value <= 0x9F) ||
                        !charChecker.matches(value) ||
                        (value == 0x5C);
                }
            });
            //markAsEscaped(escaped, isKey);
        } else {
            if (isEscaped) {
                throw new AssertionError();
            }
            FastStringBuffer buffer = new FastStringBuffer(val);
            handleInvalidCharacters(buffer);
            escaped = buffer;
        }
        return escaped.toString();
    }

    /**
     * Write a boolean value
     * @param value the boolean value to be written
     * @throws XPathException if any error occurs
     */
    public void writeBoolean(boolean value) throws XPathException {};

    /**
     * Write a null value
     *
     * @throws XPathException if any error occurs
     */
    public void writeNull() throws XPathException {};

    /**
     * Deal with invalid characters in the JSON string
     * @param buffer the JSON string
     * @throws XPathException if any error occurs
     */
    protected void handleInvalidCharacters(FastStringBuffer buffer) throws XPathException {
        //if (checkSurrogates && !liberal) {
            IntPredicate charChecker = context.getConfiguration().getValidCharacterChecker();
            for (int i = 0; i < buffer.length(); i++) {
                char ch = buffer.charAt(i);
                if (UTF16CharacterSet.isHighSurrogate(ch)) {
                    if (i + 1 >= buffer.length() || !UTF16CharacterSet.isLowSurrogate(buffer.charAt(i + 1))) {
                        substitute(buffer, i, 1, context);
                    }
                } else if (UTF16CharacterSet.isLowSurrogate(ch)) {
                    if (i == 0 || !UTF16CharacterSet.isHighSurrogate(buffer.charAt(i - 1))) {
                        substitute(buffer, i, 1, context);
                    } else {
                        int pair = UTF16CharacterSet.combinePair(buffer.charAt(i - 1), ch);
                        if (!charChecker.matches(pair)) {
                            substitute(buffer, i - 1, 2, context);
                        }
                    }
                } else {
                    if (!charChecker.matches(ch)) {
                        substitute(buffer, i, 1, context);
                    }
                }
            }
        //}
    }

    protected void markAsEscaped(CharSequence escaped, boolean isKey) throws XPathException {
        // do nothing in this class
    }

    /**
     * Replace an character or two characters within a string buffer, either by executing the replacement function,
     * or using the default Unicode replacement character
     *
     * @param buffer the string buffer, which is modified by this call
     * @param offset the position of the characters to be replaced
     * @param count the number of characters to be replaced
     * @param context the XPath context
     * @throws XPathException if the callback function throws an exception
     */
    private void substitute(FastStringBuffer buffer, int offset, int count, XPathContext context) throws XPathException {
        FastStringBuffer escaped = new FastStringBuffer(count*6);
        for (int j=0; j<count; j++) {
            escaped.append("\\u");
            String hex = Integer.toHexString(buffer.charAt(offset + j));
            while (hex.length() < 4) {
                hex = "0" + hex;
            }
            hex = hex.toUpperCase(); // cheat to get through test json-to-xml-039
            escaped.append(hex);
        }
        String replacement = replace(escaped.toString(), context);
        if (replacement.length() == count) {
            for (int j = 0; j < count; j++) {
                buffer.setCharAt(offset + j, replacement.charAt(j));
            }
        } else {
            for (int j = 0; j < count; j++) {
                buffer.removeCharAt(offset + j);
            }
            for (int j=0; j < replacement.length(); j++) {
                buffer.insert(offset + j, replacement.charAt(j));
            }
        }
    }

    /**
     * Replace an illegal XML character, either by executing the replacement function,
     * or using the default Unicode replacement character
     *
     * @param s       the string representation of the illegal character
     * @param context the XPath context
     * @return the replacement string
     * @throws XPathException if the callback function throws an exception
     */
    private String replace(String s, XPathContext context) throws XPathException {
        if (fallbackFunction != null) {
            Sequence[] args = new Sequence[1];
            args[0] = new StringValue(s);
            Sequence result = fallbackFunction.call(context, args).head();
            // TODO: await resolution of bug 28169
            Item first = result.head();
            return first == null ? "" : first.getStringValue();
        } else {
            return REPLACEMENT;
        }
    }

    public void setFallbackFunction(MapItem options, XPathContext context) throws XPathException {
        Sequence fn = options.get(new StringValue("fallback"));
        if (fn != null) {
            if (fn instanceof Function) {
                fallbackFunction = (Function) fn;
                if (fallbackFunction.getArity() != 1) {
                    throw new XPathException("Fallback function must have arity=1", "FOJS0005");
                }
//                SpecificFunctionType required = new SpecificFunctionType(
//                    new SequenceType[]{SequenceType.SINGLE_STRING}, SequenceType.SINGLE_STRING);
                SpecificFunctionType required = new SpecificFunctionType(
                        new SequenceType[]{SequenceType.SINGLE_STRING}, SequenceType.ANY_SEQUENCE);
                if (!required.matches(fallbackFunction, context.getConfiguration().getTypeHierarchy())) {
                    // TODO: await resolution of bug 28169
                    throw new XPathException("Fallback function does not match the required type", "FOJS0005");
                }
            } else {
                throw new XPathException("Value of option 'fallback' is not a function", "FOJS0005");
            }
            // Error check here
        }
    }
}

// Copyright (c) 2015 Saxonica Limited. All rights reserved.
