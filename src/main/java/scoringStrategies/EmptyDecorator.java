package scoringStrategies;

public class EmptyDecorator extends StrategyDecorator{

    public EmptyDecorator(OctiMatchStrategy underlyingStrategy){
        super(underlyingStrategy);
    }
}
