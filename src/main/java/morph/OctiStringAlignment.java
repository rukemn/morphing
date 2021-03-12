package morph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jtsadaptions.OctiLineString;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class OctiStringAlignment implements Iterable<OctiSegmentAlignment>{
    protected static final Logger logger = LogManager.getLogger();
    OctiLineString source;
    OctiLineString target;
    List<OctiSegmentAlignment> alignments = new LinkedList<>();
    protected String id;

    public CoordinateSequence getSourceSequence(){
        return source.getCoordinateSequence();
    }

    public CoordinateSequence getTargetSequence(){
        return target.getCoordinateSequence();
    }

    public Envelope getEnvelope(){
        Envelope sourceEnvelope = source.getEnvelopeInternal();
        Envelope targetEnvelope = target.getEnvelopeInternal();
        sourceEnvelope.expandToInclude(targetEnvelope);
        return new Envelope(sourceEnvelope);
    }

    public Iterator<OctiSegmentAlignment> iterator() {
        return alignments.iterator();
    }

    public String getId() {return id;}

    public void setId(String id){
        this.id = id;
    }


}
