package net.sf.saxon.style;
/**
* Abstract class representing an extension instruction
*/

public abstract class ExtensionInstruction extends StyleElement {

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public final boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:fallback
     * instruction. Note that this is only relevant if the element is an instruction.
     * @return true: this element (assuming it is an instruction) may contain an xsl:fallback
     * child instruction, which is ignored if the element is recognized.
     */

    public final boolean mayContainFallback() {
        return true;
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