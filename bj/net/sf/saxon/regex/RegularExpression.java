package net.sf.saxon.regex;

import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.SequenceIterator;

import java.io.Serializable;

/**
 * This interface represents a compiled regular expression
 */
public interface RegularExpression extends Serializable {

    /**
     * Determine whether the regular expression match a given string in its entirety
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    public boolean matches(CharSequence input);

    /**
     * Determine whether the regular expression contains a match of a given string
     * @param input the string to match
     * @return true if the string matches, false otherwise
     */

    public boolean containsMatch(CharSequence input);

    /**
     * Use this regular expression to tokenize an input string.
     * @param input the string to be tokenized
     * @return a SequenceIterator containing the resulting tokens, as objects of type StringValue
     */

    public SequenceIterator tokenize(CharSequence input);

    /**
     * Use this regular expression to analyze an input string, in support of the XSLT
     * analyze-string instruction. The resulting RegexIterator provides both the matching and
     * non-matching substrings, and allows them to be distinguished. It also provides access
     * to matched subgroups.
     */

    public RegexIterator analyze(CharSequence input);

    /**
     * Replace all substrings of a supplied input string that match the regular expression
     * with a replacement string.
     * @param input the input string on which replacements are to be performed
     * @param replacement the replacement string in the format of the XPath replace() function
     * @return the result of performing the replacement
     * @throws XPathException if the replacement string is invalid
     */

    public CharSequence replace(CharSequence input, CharSequence replacement) throws XPathException;


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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//

