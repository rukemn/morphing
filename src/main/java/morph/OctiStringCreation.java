package morph;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

public class OctiStringCreation extends OctiStringAlignment{

    Point creationPoint;
    /** Constructor for trivial insertion alignment origination from a single point (spawn)
     *
     * @param source the creation origin
     * @param target the octiLineString to be created
     * @param alignmentId an id to identify the alignment later
     */
    public OctiStringCreation(Point source, OctiLineString target, String alignmentId){
        this.source = null;
        this.target = target;
        this.creationPoint = source;
        this.id = alignmentId;
        //only deletions
        for(int i = 0; i<target.size();i++){
            OctiLineSegment ols = target.getSegment(i);
            OctiSegmentAlignment alignment= new OctiSegmentInsert(source.getCoordinate(),ols);
            alignments.add(alignment);
        }
    }

    /**Constructor for trivial insertion alignment origination from a single point (spawn)
     * This will generate a random UUID as id
     * @param source the creation origin
     * @param target the octiLineString to be created
     */
    public OctiStringCreation(Point source, OctiLineString target){
        this(source,target, UUID.randomUUID().toString());
    }


    @Override
    public Envelope getEnvelope(){
        Envelope targetEnvelope = target.getEnvelopeInternal();
        targetEnvelope.expandToInclude(creationPoint.getCoordinate());
        return new Envelope(targetEnvelope);
    }

    public CoordinateSequence getSourceSequence(){
        logger.debug("OctiStringInsertion are created from a point, not a string");
        return creationPoint.getCoordinateSequence();
    }


}
