package org.broadinstitute.hellbender.tools.walkers.qc;

import htsjdk.samtools.util.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.ReadWalker;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.exceptions.GATKException;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import picard.cmdline.programgroups.ReadDataManipulationProgramGroup;
import picard.sam.markduplicates.util.OpticalDuplicateFinder;
import picard.sam.util.PhysicalLocationInt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 blah
 */

@CommandLineProgramProperties(
        summary = org.broadinstitute.hellbender.tools.walkers.bqsr.BaseRecalibrator.USAGE_SUMMARY,
        oneLineSummary = org.broadinstitute.hellbender.tools.walkers.bqsr.BaseRecalibrator.USAGE_ONE_LINE_SUMMARY,
        programGroup = ReadDataManipulationProgramGroup.class
)
@DocumentedFeature
public final class SamToTable extends ReadWalker {
    public static final String USAGE_ONE_LINE_SUMMARY = "converts a samfile into a TSV";
    public static final String USAGE_SUMMARY = "SAM -> TSV";

    protected static final Logger logger = LogManager.getLogger(org.broadinstitute.hellbender.tools.walkers.bqsr.BaseRecalibrator.class);

     @Argument(shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME, fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME, doc = "The output table file to create", optional = false)
    private File tableOutput = null;

    @Argument(shortName="tre", fullName= "tile-reg-ex", doc="A regular expression to filter tiles by.")
    private String tileRegEx = ".*";

    @Argument( shortName = "pre",fullName = "pu-reg-ex", doc="A regular expression to filter readgroups (PU field) by.")
    private String readgroupRegEx = ".*";

    private PrintStream tableOutputStream;

    final private List<ReadElementExtractor> extractors = getExtractors();

    public boolean requiresReference() {
        return false;
    }


    @Override
    public void onTraversalStart() {
        Utils.warnOnNonIlluminaReadGroups(getHeaderForReads(), logger);

        try {
            tableOutputStream = new PrintStream(tableOutput);
        } catch (FileNotFoundException e) {
            throw new GATKException("problem trying to open stream against " + tableOutput, e);
        }

        tableOutputStream.println(extractors.stream().map(ReadElementExtractor::header).collect(Collectors.joining("\t")));
    }

    final private static OpticalDuplicateFinder opticalDuplicateFinder = new OpticalDuplicateFinder();
    final private static PhysicalLocationInt location = new PhysicalLocationInt();

    @Override
    public List<ReadFilter> getDefaultReadFilters() {
        final List<ReadFilter> filters = new ArrayList<>(6);
        filters.add(ReadFilterLibrary.NOT_SECONDARY_ALIGNMENT);
        filters.add(ReadFilterLibrary.NOT_SUPPLEMENTARY_ALIGNMENT);

        final Lazy<Pattern> readgroupPattern = new Lazy<>(()->Pattern.compile(readgroupRegEx));
        final Lazy<Pattern> tilePattern = new Lazy<>(()->Pattern.compile(tileRegEx));
        filters.add(new ReadFilter() {
            static private final long serialVersionUID = 42L;
            @Override
            public boolean test(final GATKRead read) {
                return readgroupPattern.get().matcher(read.getName()).matches();
            }
        });

        filters.add(new ReadFilter() {
            static private final long serialVersionUID = 42L;
            @Override
            public boolean test(final GATKRead read) {
                opticalDuplicateFinder.addLocationInformation(read.getName(), location);
                return tilePattern.get().matcher(String.valueOf(location.tile)).matches();
            }
        });

        return filters;
    }

    /**
     * For each read at this locus get the various covariate values and increment that location in the map based on
     * whether or not the base matches the reference at this particular location
     */
    @Override
    public void apply(GATKRead read, ReferenceContext ref, FeatureContext featureContext ) {
        final String collect = extractors.stream().map(e -> e.extractElement(read, getHeaderForReads())).collect(Collectors.joining("\t"));
        tableOutputStream.println(collect);

    }
    private List<ReadElementExtractor> getExtractors() {

        return Arrays.asList(
                new ReadElementExtractorImpl.ReadGroup(),
                new ReadElementExtractorImpl.Tile(),
                new ReadElementExtractorImpl.XCoord(),
                new ReadElementExtractorImpl.YCoord(),
                new ReadElementExtractorImpl.First(),
                new ReadElementExtractorImpl.BaseQual(),
                new ReadElementExtractorImpl.Duplicate(),
                new ReadElementExtractorImpl.Errors(),
                new ReadElementExtractorImpl.InsertSize(),
                new ReadElementExtractorImpl.Length(),
                new ReadElementExtractorImpl.Length2(),
                new ReadElementExtractorImpl.Mapped(),
                new ReadElementExtractorImpl.MappingQ()
        );
    }

    @Override
    public Object onTraversalSuccess() {
        tableOutputStream.close();
        return 0;
    }
}