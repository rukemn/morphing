package scoringStrategies;

import morph.NoPreviousPathCrossedDecorator;
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
            "Flat Score", FlatScoreStrategy::new,
            "Affine Gap", AffineGapStrategy::new
    );

    // switch to entry if theres no more overloaded functions
    private static final Map<String, Function<OctiMatchStrategy, OctiMatchStrategy>> strategyDecoratorMap = Map.of(
            "multiplicative gap cost", (underlying) -> new MultiplicativeGapDecorator(underlying),
            "none" ,  (underlying) -> new EmptyDecorator(underlying)
    );

    private static final Map<String, Function<OctiMatchStrategy, OctiMatchStrategy>> visibilityDecoratorMap = Map.of(
            "No Visibility Constraints", (underlying) -> new EmptyDecorator(underlying),
            "Target Visible", (underlying) -> new TargetVisibleDecorator(underlying),
            "Completely Visible", (underlying) -> new CompleteVisibleDecorator(underlying),
            "No VertexPath Intersect", (underlying) -> new NoPreviousPathCrossedDecorator(underlying)
    );

    /**
     * @param strategyString The Strategy name, the first one beeing the underlying strategy, rest are decorators
     * @param strategyDecorators The List of Strategy decorator names to add on top
     * @param visibilityDecorators The List of Visibility decorator names to add on top
     * @return the created Strategy
     * @throws StrategyInitializationException in case no Strategy could be instantiated from the given Strings
     */
    public static OctiMatchStrategy getStrategy(String strategyString, List<String> strategyDecorators, List<String> visibilityDecorators) throws StrategyInitializationException {
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

        for (String decoratorString : strategyDecorators) {
            logger.trace("decorating with " + decoratorString);
            Function<OctiMatchStrategy,OctiMatchStrategy> decoratorInstantiator = strategyDecoratorMap.get(decoratorString);
            if(decoratorInstantiator == null){
                logger.warn("no strategy decorator with name " + strategyString + " found.");
                throw new StrategyInitializationException("no strategy decorator with name " + strategyString + " found.");
            }
            strat = decoratorInstantiator.apply(strat);
        }
        for (String decoratorString : visibilityDecorators) {
            logger.trace("decorating with " + decoratorString);
            Function<OctiMatchStrategy,OctiMatchStrategy> decoratorInstantiator = visibilityDecoratorMap.get(decoratorString);
            if(decoratorInstantiator == null){
                logger.warn("no visibility decorator with name " + strategyString + " found.");
                throw new StrategyInitializationException("no visibility decorator with name " + strategyString + " found.");
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
        return getStrategy(baseStrategyName, new ArrayList<String>(),  new ArrayList<String>());
    }

    public static Set<String> getStrategies(){
        return baseStrategyMap.keySet();
    }

    public static Set<String> getStrategyDecorators(){
        return strategyDecoratorMap.keySet();
    }

    public static Set<String> getVisibilityDecorators(){
        return visibilityDecoratorMap.keySet();
    }

}
