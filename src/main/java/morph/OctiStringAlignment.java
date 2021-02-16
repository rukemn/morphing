package morph;

import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.Envelope;
import org.twak.utils.Pair;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class OctiStringAlignment implements Iterable<OctiSegmentAlignment> {

    OctiLineString sourceString;
    OctiLineString targetString;
    List<OctiSegmentAlignment> alignment = new LinkedList<>();

    public Envelope getEnvelope(){
        Envelope sourceEnvelope = sourceString.getEnvelopeInternal();
        Envelope targetEnvelope = targetString.getEnvelopeInternal();
        sourceEnvelope.expandToInclude(targetEnvelope);
        return new Envelope(sourceEnvelope);
    }

    public OctiStringAlignment(OctiLineString source, OctiLineString target, List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> indexAlignments) {
        this.sourceString = source;
        this.targetString = target;
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
                    seg = new OctiSegmentMatch(sourceString.getSegmentBeforeNthPoint(p.first().first()),
                            targetString.getSegmentBeforeNthPoint(p.first().second()));
                    matched = true;
                    break;
                case Delete: // delete
                    seg = new OctiSegmentDelete(sourceString.getSegmentBeforeNthPoint(p.first().first()),
                            targetString.getCoordinateN(p.first().second()));
                    deleted = true;
                    break;
                case Insert:
                    seg = new OctiSegmentInsert(sourceString.getCoordinateN(p.first().first()),
                            targetString.getSegmentBeforeNthPoint(p.first().second()));
                    inserted = true;
                    break;
                default:
                    throw new UnsupportedOperationException("No valid Operation");
            }
            alignment.add(seg);
        }

        while (als.hasNext()) {
            Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> p = als.next();
            switch (p.second()) {
                case Match: //match
                    seg = new OctiSegmentMatch(sourceString.getSegmentBeforeNthPoint(p.first().first()),
                            targetString.getSegmentBeforeNthPoint(p.first().second()));
                    break;
                case Delete://delete
                    seg = new OctiSegmentDelete(sourceString.getSegmentBeforeNthPoint(p.first().first()),
                            targetString.getCoordinateN(p.first().second()));
                    break;
                case Insert://insert
                    seg = new OctiSegmentInsert(sourceString.getCoordinateN(p.first().first()),
                            targetString.getSegmentBeforeNthPoint(p.first().second()));
                    break;
                default:
                    throw new UnsupportedOperationException("No valid Operation");
            }
            alignment.add(seg);
        }
    }

    @Override
    public Iterator iterator() {
        return alignment.iterator();
    }

    public OctiLineString getSourceString() {
        return sourceString;
    }

    public OctiLineString getTargetString() {
        return targetString;
    }
};
