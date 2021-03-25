package scoringStrategies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class ScoringStrategyFactory {

    private static final Logger logger = LogManager.getLogger();

    // switch to entry if theres no more overloaded functions
    private static final Map<String, Supplier<OctiMatchStrategy>> baseStrategyMap = Map.of(
            "BaseMatch", BaseMatchStrategy::new, //ignore any underlying strategy since this is base
            "Flat Score", FlatScoreStrategy::new
    );

    // switch to entry if theres no more overloaded functions
    private static final Map<String, Function<OctiMatchStrategy, OctiMatchStrategy>> decoratorStrategyMap = Map.of(
            //"Affine Gap", (underlying) -> new AffineGapStrategy(underlying),
            // the visibility decorators
            "No Visibility Constraints", (underlying) -> new EmptyDecorator(underlying),
            "Target Visible", (underlying) -> new TargetVisibleStrategy(underlying),
            "Completely Visible", (underlying) -> new VisibilityMatchStrategy(underlying)
    );

    /**
     * @param strategyString The Strategy name, the first one beeing the underlying strategy, rest are decorators
     * @param decorators The List of Strategy decorator names to add on top
     * @return the created Strategy
     * @throws StrategyInitializationException in case no Strategy could be instantiated from the given Strings
     */
    public static OctiMatchStrategy getStrategy(String strategyString, List<String> decorators) throws StrategyInitializationException {
        if (strategyString == null ) {
            logger.warn("no strategy supplied");
            throw new StrategyInitializationException("no strategy supplied");
        }
        //first has no underlying strategy
        logger.trace("base strategy: " + strategyString);
        Supplier<OctiMatchStrategy> baseInstantiator = baseStrategyMap.get(strategyString);
        if(baseInstantiator == null){
            logger.warn("no strategy with name " + strategyString + " found.");
            throw new StrategyInitializationException("no strategy with name " + strategyString + " found.");
        }
        OctiMatchStrategy strat = baseInstantiator.get();

        for (String decoratorString : decorators) {
            logger.trace("decorating with " + decoratorString);
            Function<OctiMatchStrategy,OctiMatchStrategy> decoratorInstantiator = decoratorStrategyMap.get(decoratorString);
            if(decoratorInstantiator == null){
                logger.warn("no decorator with name " + strategyString + " found.");
                throw new StrategyInitializationException("no decorator with name " + strategyString + " found.");
            }
            strat = decoratorInstantiator.apply(strat);
        }
        return strat;
    }

    public static OctiMatchStrategy getStrategy(String baseStrategyName) throws StrategyInitializationException {
        if(baseStrategyName == null) throw new StrategyInitializationException("no strategy supplied");
        Supplier<OctiMatchStrategy> instantiator = baseStrategyMap.get(baseStrategyName);

        if(instantiator == null) throw new StrategyInitializationException("supplied strategy not found");

        //return instantiator.get();
        return getStrategy(baseStrategyName, new ArrayList<String>());
    }

    public static Set<String> getStrategies(){
        return baseStrategyMap.keySet();
    }

    public static Set<String> getDecorators(){
        return decoratorStrategyMap.keySet();
    }

}
