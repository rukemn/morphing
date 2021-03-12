package morph;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

public class OctiStringDeletion extends OctiStringAlignment {
    Point deletionPoint;
    /**Constructor for trivial deletion alignment onto single point (despawn)
     *
     * @param source the octiLineString to be deleted
     * @param target the point on which the string is deleted
     * @param alignmentId an id to identify the alignment later
     */
    public OctiStringDeletion(OctiLineString source, Point target, String alignmentId){
        this.source = source;
        this.target = null;
        deletionPoint = target;
        //only deletions
        for(int i = 0; i<source.size();i++){
            OctiLineSegment ols = source.getSegment(i);
            OctiSegmentAlignment alignment= new OctiSegmentDelete(ols,target.getCoordinate());
            alignments.add(alignment);
        }
    }

    /** Constructor for trivial deletion alignment onto single point (despawn)
     *  This will generate a random UUID as id
     *
     * @param source the octiLineString to be deleted
     * @param target the point on which the string is deleted
     */
    public OctiStringDeletion(OctiLineString source, Point target){
        this(source,target, UUID.randomUUID().toString());
    }

    @Override
    public Envelope getEnvelope(){
        Envelope sourceEnvelope = source.getEnvelopeInternal();
        sourceEnvelope.expandToInclude(deletionPoint.getCoordinate());
        return new Envelope(sourceEnvelope);
    }

    @Override
    public CoordinateSequence getTargetSequence() {
        logger.debug("OctiStringDeletions are deleted onto a point, not a string");
        return deletionPoint.getCoordinateSequence();
    }

}
