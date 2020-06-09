package org.broadinstitute.hellbender.tools.walkers.genotyper;

import htsjdk.variant.variantcontext.Allele;
import org.broadinstitute.hellbender.utils.genotyper.AlleleList;
import org.broadinstitute.hellbender.utils.pairhmm.DragstrReferenceSTRs;
import org.broadinstitute.hellbender.utils.pairhmm.DragstrUtils;

import java.io.PrintStream;

/**
 * A wrapping interface between the various versions of genotypers so as to keep them interchangeable.
 */
public interface GenotypersModel {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <A extends Allele> GenotypingLikelihoods<A> calculateLikelihoods(AlleleList<A> genotypingAlleles, GenotypingData<A> data, final byte[] paddedReference, final int offsetForRefIntoEvent, final DragstrReferenceSTRs dragstrs);

    // Add the debug output stream managed by the engine, this should be called as part of initialization
    void addDebugOutStream(final PrintStream debugStream);
}