////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;

import java.util.ArrayList;
import java.util.List;


/**
 * This outputter is used when writing a sequence of atomic values and nodes, that
 * is, when xsl:variable is used with content and an "as" attribute. The outputter
 * builds the sequence and provides access to it. (It isn't really an outputter at all,
 * it doesn't pass the events to anyone, it merely constructs the sequence in memory
 * and provides access to it). Note that the event sequence can include calls such as
 * startElement and endElement that require trees to be built. If nodes such as attributes
 * and text nodes are received while an element is being constructed, the nodes are added
 * to the tree. Otherwise, "orphan" nodes (nodes with no parent) are created and added
 * directly to the sequence.
 * <p/>
 * <p>This class is not used to build temporary trees. For that, the ComplexContentOutputter
 * is used.</p>
 *
 * @author Michael H. Kay
 */

public final class SequenceOutputter extends SequenceWriter {

    private List<Item> list;


    /**
     * Create a new SequenceOutputter
     *
     * @param pipe the pipeline configuration
     */

    public SequenceOutputter(PipelineConfiguration pipe) {
        this(pipe, 50);
    }

    public SequenceOutputter(PipelineConfiguration pipe, int estimatedSize) {
        super(pipe);
        this.list = new ArrayList<Item>(estimatedSize);
    }

    /**
     * Allocate a SequenceOutputter. Used from generated bytecode.
     *
     * @param context  dynamic XPath context
     * @param hostLang host language (XSLT/XQuery)
     * @return the allocated SequenceOutputter
     * @see com.saxonica.ee.bytecode.util.CompilerService
     */

    /*@Nullable*/
    public static SequenceOutputter allocateSequenceOutputter(XPathContext context, int hostLang) {
        Controller controller = context.getController();
        SequenceOutputter seq = controller.allocateSequenceOutputter(20);
        seq.getPipelineConfiguration().setHostLanguage(hostLang);
        return seq;
    }

    /**
     * Clear the contents of the SequenceOutputter and make it available for reuse
     */

    public void reset() {
        list = new ArrayList<Item>(Math.min(list.size() + 10, 50));
    }

    /**
     * Method to be supplied by subclasses: output one item in the sequence.
     */

    public void write(Item item) {
        list.add(item);
    }

    /**
     * Get the sequence that has been built
     *
     * @return the value (sequence of items) that have been written to this SequenceOutputter
     */

    public Sequence getSequence() {
        switch (list.size()) {
            case 0:
                return EmptySequence.getInstance();
            case 1:
                //noinspection unchecked
                return list.get(0);
            default:
                return new SequenceExtent(list);
        }
    }

    /**
     * Get an iterator over the sequence of items that has been constructed
     *
     * @return an iterator over the items that have been written to this SequenceOutputter
     */

    public SequenceIterator iterate() {
        if (list.isEmpty()) {
            return EmptyIterator.emptyIterator();
        } else {
            return new ListIterator(list);
        }
    }

    /**
     * Get the list containing the sequence of items
     *
     * @return the list of items that have been written to this SequenceOutputter
     */

    public List<Item> getList() {
        return list;
    }

    /**
     * Get the first item in the sequence that has been built
     *
     * @return the first item in the list of items that have been written to this SequenceOutputter;
     *         or null if the list is empty.
     */

    public Item getFirstItem() {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /**
     * Get the last item in the sequence that has been built, and remove it
     *
     * @return the last item written
     */

    public Item popLastItem() {
        if (list.isEmpty()) {
            return null;
        } else {
            return list.remove(list.size() - 1);
        }
    }


}

