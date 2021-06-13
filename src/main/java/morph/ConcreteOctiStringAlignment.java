package morph;

import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.CoordinateSequence;
import org.twak.utils.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class ConcreteOctiStringAlignment extends OctiStringAlignment {

    public ConcreteOctiStringAlignment(OctiLineString source, OctiLineString target, List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> indexAlignments, String alignmentId) {
        this.source = source;
        this.target = target;
        this.id = alignmentId;
        boolean matched, inserted, deleted;
        matched = inserted = deleted = false;
        if (indexAlignments.size() == 0) return;

        //first alignment is expected to always be (0,0)-Matching, skip that
        ListIterator<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> als = indexAlignments.listIterator();
        if (als.hasNext()) als.next();

        OctiSegmentAlignment seg;

        // 1 match needed or (1 delete, 1 insert)
        while (!(matched || (inserted && deleted)) && als.hasNext()) {
            Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> p = als.next();
            switch (p.second()) {
                case Match:
                    seg = new OctiSegmentMatch(source.getSegmentBeforeNthPoint(p.first().first()),
                            target.getSegmentBeforeNthPoint(p.first().second()));
                    matched = true;
                    break;
                case Delete: // delete
                    seg = new OctiSegmentDelete(source.getSegmentBeforeNthPoint(p.first().first()),
                            target.getCoordinateN(p.first().second()));
                    deleted = true;
                    break;
                case Insert:
                    seg = new OctiSegmentInsert(source.getCoordinateN(p.first().first()),
                            target.getSegmentBeforeNthPoint(p.first().second()));
                    inserted = true;
                    break;
                default:
                    throw new UnsupportedOperationException("No valid Operation");
            }
            alignments.add(seg);
        }

        while (als.hasNext()) {
            Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> p = als.next();
            switch (p.second()) {
                case Match:
                    seg = new OctiSegmentMatch(source.getSegmentBeforeNthPoint(p.first().first()),
                            target.getSegmentBeforeNthPoint(p.first().second()));
                    break;
                case Delete:
                    seg = new OctiSegmentDelete(source.getSegmentBeforeNthPoint(p.first().first()),
                            target.getCoordinateN(p.first().second()));
                    break;
                case Insert:
                    seg = new OctiSegmentInsert(source.getCoordinateN(p.first().first()),
                            target.getSegmentBeforeNthPoint(p.first().second()));
                    break;
                default:
                    throw new UnsupportedOperationException("No valid Operation");
            }
            alignments.add(seg);
        }
    }

    public ConcreteOctiStringAlignment(OctiLineString source, OctiLineString target, List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> indexAlignments){
        this(source, target, indexAlignments, UUID.randomUUID().toString());
    }
    @Override
    public CoordinateSequence getSourceSequence() {
        return source.getCoordinateSequence();
    }

    public CoordinateSequence getTargetSequence() {
        return target.getCoordinateSequence();
    }
};
