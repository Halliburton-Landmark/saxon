package net.sf.saxon.om;

import java.util.function.Function;


public interface FocusTrackingFactory extends
        Function<SequenceIterator<? extends Item<?>>, FocusTrackingIterator<? extends Item<?>>> {


}


