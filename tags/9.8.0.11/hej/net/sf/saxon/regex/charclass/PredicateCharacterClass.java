////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


package net.sf.saxon.regex.charclass;

import net.sf.saxon.z.IntPredicate;
import net.sf.saxon.z.IntSet;

/**
 * A character class represents a set of characters for regex matching purposes. A predicate
 * character class is one where the determination of membership is established by executing
 * a function.
 */

public class PredicateCharacterClass implements CharacterClass {

    private IntPredicate predicate;

    public PredicateCharacterClass(IntPredicate predicate) {
        this.predicate = predicate;
    }

    public boolean matches(int value) {
        return predicate.matches(value);
    }

    public boolean isDisjoint(CharacterClass other) {
        return other instanceof InverseCharacterClass && other.isDisjoint(this);
    }

    public IntSet getIntSet() {
        return null;  // Not known
    }
}