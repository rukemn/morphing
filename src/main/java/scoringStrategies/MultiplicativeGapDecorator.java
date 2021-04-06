package scoringStrategies;

import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import morph.MatrixElement;
import org.locationtech.jts.geom.Coordinate;

/**
 * Todo for multiple deletions/insertions have the cost be as described in
 * https://de.wikipedia.org/wiki/Gotoh-Algorithmus
 * implemented with multiplicative gap cost
 * needed: information on previous move
 * alternativly implement GapCosts in OctiLineMatcher
 */
public class MultiplicativeGapDecorator extends StrategyDecorator{

    double IndelGapMultiplicator = 3.0;

    public MultiplicativeGapDecorator(OctiMatchStrategy underlyingStrategy){
        super(underlyingStrategy);
    }

    @Override
    public void initStrategy(OctiLineString sourceString, OctiLineString targetString) {

    }

    @Override
    public double match(MatrixElement previous,OctiLineSegment sourceSegment, OctiLineSegment targetSegment) {
        return underlyingStrategy.match(previous, sourceSegment, targetSegment); //normal match
    }

    @Override
    public double deleteOnto(MatrixElement previous,OctiLineSegment segmentToBeDeleted, Coordinate point) {
        double factor = (previous.deletionsInARow > 0)? 1.0 :IndelGapMultiplicator;
        return factor *  underlyingStrategy.deleteOnto(previous, segmentToBeDeleted, point);
    }

    @Override
    public double createFrom(MatrixElement previous,Coordinate creationPoint, OctiLineSegment segmentToBeCreated) {
        double factor = (previous.insertionsInARow > 0)? 1.0 :IndelGapMultiplicator;
        return factor *  underlyingStrategy.createFrom(previous, creationPoint, segmentToBeCreated);
    }
}
