////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.AtomicMatchKey;
import net.sf.saxon.expr.sort.CodepointMatchKey;
import net.sf.saxon.functions.Count;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * This class implements an XPath map item as a view of an XSLT key index. It is possible to specify a minimum
 * and maximum key value to be included in the map, and keys are returned in sorted order.
 * <p>At present range keys are only available for string-valued keys using the Unicode codepoint collating sequence.</p>
 */

public class RangeKey implements MapItem {

    private CodepointMatchKey min;
    private CodepointMatchKey max;
    private TreeMap<AtomicMatchKey, List<NodeInfo>> index;

    public RangeKey(String min, String max, TreeMap<AtomicMatchKey, List<NodeInfo>> index) {
        this.min = min == null ? null : new CodepointMatchKey(min);
        this.max = max == null ? null : new CodepointMatchKey(max);
        this.index = index;
    }

    /**
     * Get an entry from the Map
     * @param key     the value of the key
     * @return the value associated with the given key, or null if the key is not present in the map
     */
    public GroundedValue<? extends Item<?>> get(AtomicValue key)  {
        CodepointMatchKey k = new CodepointMatchKey(key.getStringValue());
        if ((min == null || min.compareTo(k) <= 0) &&
                (max == null || max.compareTo(k) >= 0)) {
            List<NodeInfo> nodes = index.get(k);
            if (nodes == null) {
                return EmptySequence.getInstance();
            } else {
                return SequenceExtent.makeSequenceExtent(nodes);
            }
        }
        return EmptySequence.getInstance();
    }

    /**
     * Get the size of the map
     *
     * @return the number of keys/entries present in this map
     */
    public int size() {
        try {
            return Count.count(keys());
        } catch (XPathException err) {
            return 0;
        }
    }

    /**
     * Ask whether the map is empty
     *
     * @return true if and only if the size of the map is zero
     */
    public boolean isEmpty() {
        return keys().next() == null;
    }

    /**
     * Get the set of all key values in the map.
     *
     * @return a set containing all the key values present in the map.
     */
    public AtomicIterator keys() {
        return new RangeKeyIterator();
    }

    /**
     * Get the set of all key-value pairs in the map
     *
     * @return an iterator over the key-value pairs
     */
    public Iterable<KeyValuePair> keyValuePairs() {
        // TODO: avoid constructing the list in memory
        List<KeyValuePair> list = new ArrayList<>();
        keys().forEach(key -> list.add(new KeyValuePair(key, get(key))));
        return list;
    }

    /**
     * Remove an entry from the map
     *
     * @param key     the key of the entry to be removed
     * @return a new map in which the requested entry has been removed; or this map
     *         unchanged if the specified key was not present
     */
    public MapItem remove(AtomicValue key) {
        return HashTrieMap.copy(this).remove(key);
    }

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return ErrorType.getInstance() (the type with no instances)
     */
    public UType getKeyUType() {
        return UType.STRING;
    }

    /**
     * Create a new map containing the existing entries in the map plus an additional entry,
     * without modifying the original. If there is already an entry with the specified key,
     * this entry is replaced by the new entry.
     *
     * @param key   the key of the new entry
     * @param value the value associated with the new entry
     * @return the new map containing the additional entry
     */
    @Override
    public MapItem addEntry(AtomicValue key, GroundedValue<?> value) {
        return HashTrieMap.copy(this).addEntry(key, value);
    }

    /**
     * Ask whether the map conforms to a given map type
     *
     * @param keyType   the required keyType
     * @param valueType the required valueType
     * @param th        the type hierarchy cache for the configuration
     * @return true if the map conforms to the required type
     */
    @Override
    public boolean conforms(AtomicType keyType, SequenceType valueType, TypeHierarchy th) {
        AtomicIterator keyIter = keys();
        AtomicValue key;
        while ((key = keyIter.next()) != null) {
            Sequence value = get(key);
            try {
                if (!valueType.matches(value, th)) {
                    return false;
                }
            } catch (XPathException e) {
                throw new AssertionError(e);
            }
        }
        return true;
    }

    /**
     * Get the type of the map. This method is used largely for diagnostics, to report
     * the type of a map when it differs from the required type.
     *
     * @param th the type hierarchy (not used)
     * @return the type of this map
     */
    @Override
    public MapType getItemType(TypeHierarchy th) {
        return new MapType(BuiltInAtomicType.STRING, SequenceType.NODE_SEQUENCE);
    }

    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */
    public MapType getFunctionItemType() {
        return new MapType(BuiltInAtomicType.STRING, SequenceType.NODE_SEQUENCE);
    }

    /**
     * Get a description of this function for use in error messages. For named functions, the description
     * is the function name (as a lexical QName). For others, it might be, for example, "inline function",
     * or "partially-applied ends-with function".
     *
     * @return a description of the function for use in error messages
     */
    public String getDescription() {
        return "range key";
    }

    /**
     * Test whether this FunctionItem is deep-equal to another function item,
     * under the rules of the deep-equal function
     *
     * @param other    the other function item
     * @param context  the dynamic evaluation context
     * @param comparer the object to perform the comparison
     * @param flags    options for how the comparison is performed
     * @return true if the two function items are deep-equal
     */
    public boolean deepEquals(Function other, XPathContext context, AtomicComparer comparer, int flags)  {
        if (other instanceof RangeKey) {
            RangeKey rk = (RangeKey) other;
            return min.equals(rk.min) && max.equals(rk.max) && index.equals(rk.index);
        } else {
            return false;
        }
    }

    /**
     * Output information about this function item to the diagnostic explain() output
     */
    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("range-key-map");
        out.emitAttribute("size", size() + "");
        out.endElement();
    }

    public boolean isTrustedResultType() {
        return false;
    }

    public String toString() {
        return MapItem.mapToString(this);
    }

    private class RangeKeyIterator implements AtomicIterator {

        int pos = 0;
        CodepointMatchKey curr = null;
        CodepointMatchKey top;

        public RangeKeyIterator() {
            top = (CodepointMatchKey) (max == null ? index.lastKey() : index.floorKey(max));
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next Item. If there are no more nodes, return null.
         */
        public StringValue next() {
            if (pos <= 0) {
                if (pos < 0) {
                    return null;
                }
                if (min == null) {
                    curr = (CodepointMatchKey) index.firstKey();
                } else {
                    curr = (CodepointMatchKey) index.ceilingKey(min);
                    if(curr != null && max != null && curr.compareTo(max) > 0) {
                        curr = null;
                    }
                }
            } else if (curr.equals(top)) {
                curr = null;
            } else {
                curr = (CodepointMatchKey) index.higherKey(curr);
            }
            if (curr == null) {
                pos = -1;
                return null;
            } else {
                pos++;
                return new StringValue(curr.getValue());
            }
        }

    }
}

// Copyright (c) 2012 Saxonica Limited.