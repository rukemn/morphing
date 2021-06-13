package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

/**
 * Subtracts the edge length from the score to give longer segments more weight
 *
 */
public class WeightedEdgelengthDecorator extends StrategyDecorator{

    public WeightedEdgelengthDecorator(OctiMatchStrategy underlyingStrategy){
        super(underlyingStrategy);
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {
        //nothing here
    }

    @Override
    public double match(MatrixElement previous,OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return underlyingStrategy.match(previous, sourceSegment, targetSegment) - sourceSegment.getLength() - targetSegment.getLength(); //normal match
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point) {
        return underlyingStrategy.deleteOnto(previous, segmentToBeDeleted, point) - segmentToBeDeleted.getLength();
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        return underlyingStrategy.createFrom(previous, creationPoint, segmentToBeCreated) - segmentToBeCreated.getLength();
    }
}
