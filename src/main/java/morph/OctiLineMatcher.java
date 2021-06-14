package morph;

import io.FileParseException;
import io.PolygonExtractor;
import io.PolygonExtractorInterface;
import jtsadaptions.OctiGeometryFactory;
import jtsadaptions.OctiLineSegment;
import jtsadaptions.OctiLineString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.twak.utils.Pair;
import scoringStrategies.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class OctiLineMatcher {

    private static final Logger logger = LogManager.getLogger();

    public static final int MATCH = 0;
    public static final int DELETE = 1;
    public static final int INSERT = 2;
    public static final double IMPOSSIBLE = Double.POSITIVE_INFINITY;

    //backtracking
    public static final int MATCH_ORIGIN_OP = 3;
    public static final int DELETE_ORIGIN_OP = 4;
    public static final int INSERT_ORIGIN_OP = 5;

    private final OctiLineString source;
    private final OctiLineString target;

    //whether source and target should be swapped after invoking the constructor
    public boolean sourceAndTargetSwapped = false;

    private MatrixElement[][] objectScores;

    public static void main(String[] args) {
        String svgPath;
        if (args.length == 1) { //interpret arg as path
            svgPath = args[0];
        } else {
            svgPath = "src/main/resources/squareTriangle.svg";
        }
        PolygonExtractorInterface extractor = new PolygonExtractor();
        try {
            extractor.parseFile(Paths.get(svgPath).toUri());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FileParseException e) {
            e.printStackTrace();
            logger.warn("couldnt parse file content");
        }

        Geometry srcGeom = extractor.getNthGeometry(0); //ogf.createOctiLineString(importer.getNthOctiLineString(0).getExteriorRing().getCoordinateSequence());
        Geometry tarGeom = extractor.getNthGeometry(1); //ogf.createOctiLineString(importer.getNthOctiLineString(1).getExteriorRing().getCoordinateSequence());

        OctiLineString src, tar;
        try {
            src = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(srcGeom);
            tar = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(tarGeom);

        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("no valid Geometry to extract an OctiLineString from");
            return;
        }

        OctiLineSegment.setStrategy(new CompleteVisibleDecorator(new VertexDistanceStrategy()), src, tar);
        OctiLineMatcher matcher = new OctiLineMatcher(src, tar);

        try {
            matcher.backtrack();
        } catch (NoMinimumOperationException e) {
            e.printStackTrace();
            logger.warn("no valid match");
        }
    }

    /**
     * Brings the OctiLineStrings in a configuration, such that the algorithm can work on it.
     * <p>
     * Assumptions:
     * <ul>
     *     <li>OctiLineStrings do not self-intersect</li>
     * </ul>
     * <p>
     * Config guaranties:
     *
     *  <ul>
     *      <li>if {@link OctiLineMatcher#sourceAndTargetSwapped} is <i>true</i> the strings will be swapped before returning</li>
     *      <li>OctiLineStrings are in clockwise orientation</li>
     *      <li>OctiLineStrings are rotated such that the starting points are the points returned by {@link OctiLineMatcher#determineBestStartingPoint}</li>
     *  </ul>
     *
     * @param src the source string
     * @param tar the target string
     * @returns the pair of configured {@link OctiLineString}s,
     * {@link Pair#first()} being the source,
     * {@link Pair#second()} being the target,
     */
    private Pair<OctiLineString, OctiLineString> createBaseConfig(OctiLineString src, OctiLineString tar, String startpointStrategy) {

        //checks for self-intersect, strings may be non-closed though
        if (!src.isSimple()) throw new IllegalArgumentException("src input not simple");
        if (!tar.isSimple()) throw new IllegalArgumentException("tar input not simple");

        if (sourceAndTargetSwapped) {
            OctiLineString tempSource = src;
            src = tar;
            tar = tempSource;
            logger.trace("swapped source and target");
        }

        if (src.isClosed() && tar.isClosed()) {
            if (Orientation.isCCW(src.getCoordinateSequence())) {
                logger.trace("source is oriented in counter-clockwise order --> making it clockwise");
                src = (OctiLineString) src.reverse();
            }
            if (Orientation.isCCW(tar.getCoordinateSequence())) {
                logger.trace("target is oriented in counter-clockwise order --> making it clockwise");
                tar = (OctiLineString) tar.reverse();
            }
        }

        //considers the case of non-closedness
        int[] startpoints = determineBestStartingPoint(src, tar, startpointStrategy);
        src = src.makeNthPointTheFirst(startpoints[0]);
        tar = tar.makeNthPointTheFirst(startpoints[1]);

        return new Pair<>(src, tar);
    }

    /**
     * Creates a new instance with the specified OctiLineStrings
     *
     * @param sourceLineString the source String
     * @param targetLineString the target String
     */
    public OctiLineMatcher(OctiLineString sourceLineString, OctiLineString targetLineString) {
        this(sourceLineString, targetLineString, new VertexDistanceStrategy(), "Closest Point");
    }

    public OctiLineMatcher(OctiLineString sourceLineString, OctiLineString targetLineString, OctiMatchStrategy strategy) {
        this(sourceLineString, targetLineString, strategy, "Closest Point");
    }

    public OctiLineMatcher(OctiLineString sourceLineString, OctiLineString targetLineString, OctiMatchStrategy strategy, String startingPointStrategy) {
        Pair<OctiLineString, OctiLineString> baseConfig = createBaseConfig(sourceLineString, targetLineString, startingPointStrategy);
        source = baseConfig.first();
        target = baseConfig.second();

        //must be after bringing the strings into start configuration
        OctiLineSegment.setStrategy(strategy, source, target);

        initBoard();
        iterateBoard();
    }


    /**
     * Determines the starting Points of the alignment by selecting the two appropriate points, defined by the startPointStrategy
     * Accepted strategy Strings are :
     * <ul>
     *     <li>"Closest Points": selects the points with minimum distance</li>
     *     <li>"Corner:"": selects the points, which are on the which are on the specified corner with respect to their polygon</li>
     *     <li>"Best Match Segment" : Select the first points of the edges rated with the overall best Match-Score (using Vertex Distance)</li>
     * </ul>
     * with minimum distance to each other
     *
     * @param first  the first LineString
     * @param second the second LineString
     * @returns int array of size 2  with the preferred starting points,
     * with the first element corresponding to the index of first,
     * second element corresponding to the index of second
     */
    private int[] determineBestStartingPoint(OctiLineString first, OctiLineString second, String startPointStrategy) {
        int[] startPoint_indices = {0, 0};
        if (!first.isClosed() || !second.isClosed()) {
            logger.warn("At least one linestring is not closed, returning the first points");
            return new int[]{0, 0};
        }

        if (startPointStrategy.equals("Closest Points")) { // 0(n²)
            logger.info("determining starting point by minimal distance");
            double minDistance = first.getCoordinateN(0).distance(second.getCoordinateN(0));
            for (int i = 0; i < first.getNumPoints(); i++) {
                Coordinate from = first.getCoordinateN(i);
                for (int j = 0; j < second.getNumPoints(); j++) {
                    Coordinate to = second.getCoordinateN(j);
                    if (from.distance(to) < minDistance) {
                        minDistance = from.distance(to);
                        startPoint_indices[0] = i;
                        startPoint_indices[1] = j;
                    }
                }
            }
        } else if (startPointStrategy.substring(0, 7).equals("Corner:")) { // check the corners, O(n + m)
            boolean right, top;
            switch (startPointStrategy) {
                case "Corner: Bottom left": // O( max(n.m))
                    logger.info("determining starting point by selecting the bottom left point");
                    right = false;
                    top = false;
                    break;
                case "Corner: Bottom right":
                    logger.info("determining starting point by selecting the bottom right point");
                    right = true;
                    top = false;
                    break;
                case "Corner: Top left":
                    logger.info("determining starting point by selecting the top left point");
                    right = false;
                    top = true;
                    break;
                case "Corner: Top right":
                    logger.info("determining starting point by selecting the top right point");
                    right = true;
                    top = true;
                    break;
                default: // the default/error case
                    // some error message
                    logger.warn("Could not handle corner selection, returning Bottom left");
                    right = false;
                    top = false;
            }
            Pair<Integer, Double> firstMinima = getMinimumItem(first, right, top);
            Pair<Integer, Double> secondMinima = getMinimumItem(second, right, top);
            startPoint_indices[0] = firstMinima.first();
            startPoint_indices[1] = secondMinima.first();

        } else if (startPointStrategy.equals("Best Match Segment")) { // O(n²)
            try {
                logger.info("determining starting point by selecting segment-startpoint of best-scoring matching segments (Vertex Distance)");
                OctiMatchStrategy segmentStrategy = ScoringStrategyFactory.getStrategy("Vertex Distance");
                Double minScore = null; // java needs this to be initialized, value doesn't matter though
                Double score;
                for (int firstVertexIndex = 1; firstVertexIndex < first.getNumPoints(); firstVertexIndex++) {
                    for (int secondVertexIndex = 1; secondVertexIndex < second.getNumPoints(); secondVertexIndex++) {
                        OctiLineSegment firstSegment = first.getSegmentBeforeNthPoint(firstVertexIndex);
                        OctiLineSegment secondSegment = second.getSegmentBeforeNthPoint(secondVertexIndex);

                        //only check if match possible
                        if (firstSegment.getOrientation() == secondSegment.getOrientation()) {
                            score = segmentStrategy.match(null, firstSegment, secondSegment);
                            logger.trace("matchable " + firstVertexIndex + ", " + secondVertexIndex + ": " + score);

                            //first occurence will be null, all later evaluations will do correct min-check
                            if (minScore == null || score < minScore) {
                                //startpoint will be the beginning of the minScore-Match-Segement, opposed to segmentEndpoint
                                startPoint_indices[0] = firstVertexIndex - 1; //
                                startPoint_indices[1] = secondVertexIndex - 1;
                                minScore = score;
                            }
                        }
                    }
                }

            } catch (StrategyInitializationException e) {
                logger.warn("Could not handle strategy selection, returning first points");
                e.printStackTrace();
            }
        }

        logger.info("starting points are: " + startPoint_indices[0] + " (source) and " + startPoint_indices[1] + " (target)");
        return startPoint_indices;
    }


    private Pair<Integer, Double> getMinimumItem(LineString lineString, boolean subtractFromMaxX, boolean subtractFromMaxY) {
        double substractorX = subtractFromMaxX ? -1.0 : 1.0;
        double substractorY = subtractFromMaxY ? -1.0 : 1.0;
        double minuendX = subtractFromMaxX ? getMaxXValue(lineString) : 0.0;
        double minuendY = subtractFromMaxY ? getMaxYValue(lineString) : 0.0;
        double min, currentmin;
        Integer minIndex = 0;
        min = (minuendX + substractorX * lineString.getCoordinateN(0).getX()) + (minuendY + substractorY * lineString.getCoordinateN(0).getY());
        for (int i = 1; i < lineString.getNumPoints(); i++) {
            currentmin = (minuendX + substractorX * lineString.getCoordinateN(i).getX()) + (minuendY + substractorY * lineString.getCoordinateN(i).getY());
            if (currentmin < min) {
                min = currentmin;
                minIndex = i;
            }
        }
        return new Pair<>(minIndex, min);

    }

    private Double getMaxYValue(LineString ls) {
        if (ls == null || ls.isEmpty()) return null;
        double max = ls.getCoordinateN(0).getY();
        for (int i = 1; i < ls.getNumPoints(); i++) {
            max = Math.min(ls.getCoordinateN(0).getY(), max);
        }
        return max;
    }

    private Double getMaxXValue(LineString ls) {
        if (ls == null || ls.isEmpty()) return null;
        double max = ls.getCoordinateN(0).getX();
        for (int i = 1; i < ls.getNumPoints(); i++) {
            max = Math.min(ls.getCoordinateN(0).getX(), max);
        }
        return max;
    }


    public OctiLineString getSource() {
        return source;
    }

    public OctiLineString getTarget() {
        return target;
    }

    /**
     * Intended to be called after the alignment was calculated, to give insight into the quality of the alignment
     *
     * @return the score the calculated Alignment was valued with, the lower the better
     * @throws NoMinimumOperationException in case no valid Alignment could be found
     */
    public double getEndScore() throws NoMinimumOperationException {
        int bestOperation = 0;
        try {
            bestOperation = minimumOperation(objectScores[source.size()][target.size()].matchScore,
                    objectScores[source.size()][target.size()].deleteScore,
                    objectScores[source.size()][target.size()].insertScore);
        } catch (NoMinimumOperationException e) {
            logger.info("No valid match possible");
            throw e;
        }

        //logger.info("best match Score: " + objectScores[source.size()][target.size()]);
        if (bestOperation == MATCH) {
            return objectScores[source.size()][target.size()].matchScore;
        } else if (bestOperation == DELETE) {
            return objectScores[source.size()][target.size()].deleteScore;
        } else { //if(bestOperation == INSERT)
            return objectScores[source.size()][target.size()].insertScore;
        }

    }

    /**
     * Backtracks the Double array to determine which path through the matrix led to the best score
     *
     * @return a pair made of (Source-Index, Target-Index) , wrapped in another Pair with the second Part containing the operation,
     * that led to the specified index position
     * @throws NoMinimumOperationException in case no valid Alignment could be found
     */
    private List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> backtrack() throws NoMinimumOperationException {
        List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> moves = new ArrayList<>();

        int sourceIndex = source.size();
        int targetIndex = target.size();

        int opIndex;

        try {
            opIndex = minimumOperation(
                    objectScores[sourceIndex][targetIndex].matchScore,
                    objectScores[sourceIndex][targetIndex].deleteScore,
                    objectScores[sourceIndex][targetIndex].insertScore);
        } catch (NoMinimumOperationException e) {
            logger.warn("could not backtrack, no valid match found");
            throw e;
        }

        OctiSegmentAlignment.Operation op = translate(opIndex);

        Pair<Integer, Integer> position = new Pair<>(sourceIndex, targetIndex);
        Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> move = new Pair<>(position, op);
        moves.add(move);

        while ((sourceIndex != 0 || targetIndex != 0)) {
            switch (opIndex) {
                case MATCH:
                    // I was a match, and to get here OP was used
                    opIndex = (int) objectScores[sourceIndex][targetIndex].matchOriginOp;
                    sourceIndex -= 1;
                    targetIndex -= 1;
                    break;
                case DELETE:
                    opIndex = (int) objectScores[sourceIndex][targetIndex].deleteOriginOp;
                    sourceIndex -= 1;
                    break;
                case INSERT:
                    opIndex = (int) objectScores[sourceIndex][targetIndex].insertOriginOp;
                    targetIndex -= 1;
                    break;
                default:
                    logger.error("cant happen, unknown operation");
                    assert false;

            }
            op = translate(opIndex);

            position = new Pair<>(sourceIndex, targetIndex);
            move = new Pair<>(position, op);

            moves.add(move);
        }

        Collections.reverse(moves);
        for (Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> m : moves) {
            logger.trace("To get to (" + m.first().first() + "," + m.first().second() + ") -> " + m.second().name());
        }
////////////////////////////////////////////
        List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> moves2 = new ArrayList<>();
        sourceIndex = source.size();
        targetIndex = target.size();
        MatrixElement el = objectScores[sourceIndex][targetIndex];
        OctiSegmentAlignment.Operation bestOperation = el.minOperation;

        while (bestOperation != null) {
            el = el.getBestPreviousElement(); //basema there is a prev el so get it
            moves2.add(new Pair<>(new Pair<>(sourceIndex, targetIndex), bestOperation));
            switch (bestOperation) {
                case Match:
                    sourceIndex--;
                    targetIndex--;
                    break;
                case Delete:
                    sourceIndex--;
                    break;
                case Insert:
                    targetIndex--;
                    break;
            }
            bestOperation = el.minOperation; // setup to check if there is a prev el

        }
        if (el.equals(objectScores[0][0])) {
            moves2.add(new Pair<>(new Pair<>(0, 0), OctiSegmentAlignment.Operation.Match));
            logger.trace("arrived at start");
        } else {
            logger.trace("cant backtrace");
        }
        //bestOperation is null
        logger.trace("src index" + sourceIndex + " tar index " + targetIndex);
        logger.trace("object without prev: " + el);

        Collections.reverse(moves2);
        for (Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> m : moves2) {
            logger.trace("To get to (" + m.first().first() + "," + m.first().second() + ") -> " + m.second().name());
        }
        boolean same = true;
        for (int i = 0; i < moves.size(); i++) {
            Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> m = moves.get(i);
            Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> m2 = moves2.get(i);
            if (m.first().first() != m2.first().first() || m.first().second() != m2.first().second() || m.second().name() != m2.second().name()) {
                same = false;
            }
        }
        logger.trace("same: " + same);
        return moves2;
    }

    /**
     * Helper function, maps 0->Match, 1->Delete, 2->Insert
     *
     * @param code the integer Code
     * @return the Corresponding Operation
     */
    private OctiSegmentAlignment.Operation translate(int code) {
        switch (code) {
            case MATCH:
                return OctiSegmentAlignment.Operation.Match;
            case DELETE:
                return OctiSegmentAlignment.Operation.Delete;
            case INSERT:
                return OctiSegmentAlignment.Operation.Insert;
        }
        logger.error("invalid code");
        return null;
    }

    /**
     * Queries for the OctiStringAlignment - Object in which the calculated alignment is encapsulated
     *
     * @return the Alignment
     * @throws NoMinimumOperationException in case no valid Alignment could be found
     */
    public OctiStringAlignment getAlignment() throws NoMinimumOperationException {
        List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> alignmentIndicies = null;
        try {
            alignmentIndicies = backtrack();
        } catch (NoMinimumOperationException e) {
            e.printStackTrace();
            logger.warn("no valid match");
            throw e;
        }

        return new ConcreteOctiStringAlignment(source, target, alignmentIndicies);
    }

    /**
     * Inits the starting point (0 0) , aswell as the 0-th row and the 0-th column.
     * These only consist of insertion chains (i.e. columns) and  deletion chains (i.e. rows)
     */
    private void initBoard() {
        logger.info("init Board");
        objectScores = new MatrixElement[source.size() + 1][target.size() + 1];

        objectScores[0][0] = new MatrixElement(source.getCoordinateN(0), target.getCoordinateN(0));
        objectScores[0][0].matchScore = 0.0;
        objectScores[0][0].insertScore = IMPOSSIBLE;
        objectScores[0][0].deleteScore = IMPOSSIBLE;

        //there's no origin for the origin
        objectScores[0][0].matchOriginOp = IMPOSSIBLE;
        objectScores[0][0].deleteOriginOp = IMPOSSIBLE;
        objectScores[0][0].insertOriginOp = IMPOSSIBLE;

        for (int i = 1; i <= source.size(); i++) {
            objectScores[i][0] = new MatrixElement(source.getCoordinateN(i), target.getCoordinateN(0));
            objectScores[i][0].matchScore = IMPOSSIBLE;
            objectScores[i][0].insertScore = IMPOSSIBLE;
            objectScores[i][0].matchOriginOp = IMPOSSIBLE;
            objectScores[i][0].insertOriginOp = IMPOSSIBLE;

            // calc DELETE costs
            OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(i);
            Coordinate targetStartPoint = target.getCoordinateN(0);
            if (i == 1) {
                objectScores[i][0].deleteScore = OctiLineSegment.deleteOnto(objectScores[i - 1][0], sourceSegment, targetStartPoint);
                objectScores[i][0].deleteOriginOp = MATCH;
            } else {

                objectScores[i][0].deleteScore = objectScores[i - 1][0].deleteScore + OctiLineSegment.deleteOnto(objectScores[i - 1][0], sourceSegment, targetStartPoint);
                //make backtracking possible only if the previous deletion wasn't already impossible
                if (objectScores[i][0].deleteScore != IMPOSSIBLE && objectScores[i - 1][0].deleteScore != IMPOSSIBLE) {
                    objectScores[i][0].deleteOriginOp = DELETE;
                } else {
                    objectScores[i][0].deleteOriginOp = IMPOSSIBLE;
                }
            }
            if (objectScores[i][0].deleteScore != IMPOSSIBLE) {
                objectScores[i][0].minOperation = OctiSegmentAlignment.Operation.Delete;
                objectScores[i][0].setPrevious(objectScores[i - 1][0], OctiSegmentAlignment.Operation.Delete);
            }
        }
        for (int j = 1; j <= target.size(); j++) {
            objectScores[0][j] = new MatrixElement(source.getCoordinateN(0), target.getCoordinateN(j));
            objectScores[0][j].matchScore = IMPOSSIBLE;
            objectScores[0][j].deleteScore = IMPOSSIBLE;
            objectScores[0][j].matchOriginOp = IMPOSSIBLE;
            objectScores[0][j].deleteOriginOp = IMPOSSIBLE;

            // calc INSERT costs
            OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(j);
            Coordinate sourceStartPoint = source.getCoordinateN(0);

            if (j == 1) {
                objectScores[0][j].insertScore = OctiLineSegment.createFrom(objectScores[0][j - 1], sourceStartPoint, targetSegment);
                objectScores[0][j].insertOriginOp = MATCH;
            } else {
                //origin can only be an insert operation
                objectScores[0][j].insertScore = objectScores[0][j - 1].insertScore + OctiLineSegment.createFrom(objectScores[0][j - 1], sourceStartPoint, targetSegment);

                //make backtracking possible only if the previous insertion wasn't already impossible
                if (objectScores[0][j - 1].insertScore != IMPOSSIBLE && objectScores[0][j].insertScore != IMPOSSIBLE) {
                    objectScores[0][j].insertOriginOp = INSERT;
                } else {
                    objectScores[0][j].insertOriginOp = IMPOSSIBLE;
                }
            }
            if (objectScores[0][j].insertScore != IMPOSSIBLE) {
                objectScores[0][j].minOperation = OctiSegmentAlignment.Operation.Insert;
                objectScores[0][j].setPrevious(objectScores[0][j - 1], OctiSegmentAlignment.Operation.Insert);
            }
        }
    }

    /**
     * Iterates the board filling in all values.
     * Rows and columns are filled index-increasing order, origination from the diagonal lines elements
     */
    private void iterateBoard() {
        logger.info("iterating board");
        //int smallerDiag = Math.min(scores.length, scores[0].length);
        int smallerDiag = Math.min(objectScores.length, objectScores[0].length);
        MatrixElement previous = null;
        for (int diagonalIndex = 1; diagonalIndex < smallerDiag; diagonalIndex++) {
            //diagonal elements
            logger.trace("calculating (" + diagonalIndex + ", " + diagonalIndex + ")");
            objectScores[diagonalIndex][diagonalIndex] = new MatrixElement(source.getCoordinateN(diagonalIndex), target.getCoordinateN(diagonalIndex));

            objectScores[diagonalIndex][diagonalIndex].matchScore = calcMatchingScore(diagonalIndex, diagonalIndex);
            objectScores[diagonalIndex][diagonalIndex].deleteScore = calcDeletionScore(diagonalIndex, diagonalIndex);
            objectScores[diagonalIndex][diagonalIndex].insertScore = calcInsertionScore(diagonalIndex, diagonalIndex);
            objectScores[diagonalIndex][diagonalIndex].setMinimumOperation();
            if (objectScores[diagonalIndex][diagonalIndex].minOperation != null) {
                switch (objectScores[diagonalIndex][diagonalIndex].minOperation) {
                    case Match:
                        previous = objectScores[diagonalIndex - 1][diagonalIndex - 1];
                        break;
                    case Delete:
                        previous = objectScores[diagonalIndex - 1][diagonalIndex];
                        break;
                    case Insert:
                        previous = objectScores[diagonalIndex][diagonalIndex - 1];
                        break;
                    default:
                        previous = null;
                }
                objectScores[diagonalIndex][diagonalIndex].setPrevious(previous, objectScores[diagonalIndex][diagonalIndex].minOperation);
            }

            //fill rows starting from diagonal line
            for (int columnIndex = diagonalIndex + 1; columnIndex < objectScores.length; columnIndex++) {
                logger.trace("calculating (" + columnIndex + ", " + diagonalIndex + ")");
                objectScores[columnIndex][diagonalIndex] = new MatrixElement(source.getCoordinateN(columnIndex), target.getCoordinateN(diagonalIndex));

                objectScores[columnIndex][diagonalIndex].matchScore = calcMatchingScore(columnIndex, diagonalIndex);
                objectScores[columnIndex][diagonalIndex].deleteScore = calcDeletionScore(columnIndex, diagonalIndex);
                objectScores[columnIndex][diagonalIndex].insertScore = calcInsertionScore(columnIndex, diagonalIndex);
                objectScores[columnIndex][diagonalIndex].setMinimumOperation();
                if (objectScores[columnIndex][diagonalIndex].minOperation != null) {
                    switch (objectScores[columnIndex][diagonalIndex].minOperation) {
                        case Match:
                            previous = objectScores[columnIndex - 1][diagonalIndex - 1];
                            break;
                        case Delete:
                            previous = objectScores[columnIndex - 1][diagonalIndex];
                            break;
                        case Insert:
                            previous = objectScores[columnIndex][diagonalIndex - 1];
                            break;
                        default:
                            previous = null;
                    }
                    objectScores[columnIndex][diagonalIndex].setPrevious(previous, objectScores[columnIndex][diagonalIndex].minOperation);
                }
            }
            //fill colums starting from diagonal line
            for (int rowIndex = diagonalIndex + 1; rowIndex < objectScores[0].length; rowIndex++) {
                logger.trace("calculating (" + diagonalIndex + ", " + rowIndex + ")");
                objectScores[diagonalIndex][rowIndex] = new MatrixElement(source.getCoordinateN(diagonalIndex), target.getCoordinateN(rowIndex));

                objectScores[diagonalIndex][rowIndex].matchScore = calcMatchingScore(diagonalIndex, rowIndex);
                objectScores[diagonalIndex][rowIndex].deleteScore = calcDeletionScore(diagonalIndex, rowIndex);
                objectScores[diagonalIndex][rowIndex].insertScore = calcInsertionScore(diagonalIndex, rowIndex);
                objectScores[diagonalIndex][rowIndex].setMinimumOperation();
                if (objectScores[diagonalIndex][rowIndex].minOperation != null) {
                    switch (objectScores[diagonalIndex][rowIndex].minOperation) {
                        case Match:
                            previous = objectScores[diagonalIndex - 1][rowIndex - 1];
                            break;
                        case Delete:
                            previous = objectScores[diagonalIndex - 1][rowIndex];
                            break;
                        case Insert:
                            previous = objectScores[diagonalIndex][rowIndex - 1];
                            break;
                        default:
                            previous = null;
                    }
                    objectScores[diagonalIndex][rowIndex].setPrevious(previous, objectScores[diagonalIndex][rowIndex].minOperation);
                }
            }
        }
    }


    /**
     * calculates the score for the case that the OctiLineStrings end in the
     * points sourceIndex and targetIndex with a MATCH - operation
     *
     * @param sourceIndex the index of the source OctiLineSegment's endpoint
     * @param targetIndex the index of the target OctiLineSegment's endpoint
     * @return the calculated score
     */
    private double calcMatchingScore(int sourceIndex, int targetIndex) {
        OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(sourceIndex);
        OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(targetIndex);

        OctiLineSegment.Orientation sourceOrientaion = sourceSegment.getOrientation();
        OctiLineSegment.Orientation targetOrientaion = targetSegment.getOrientation();
        logger.trace("checking segments " + sourceIndex + " (" + sourceOrientaion + ") and " + targetIndex + " (" + targetOrientaion + ")");

        //only matches between same-oriented segments are allowed
        if (sourceOrientaion != targetOrientaion) return IMPOSSIBLE; //P

        double prevMatchScore = objectScores[sourceIndex - 1][targetIndex - 1].matchScore;
        double prevDeleteScore = objectScores[sourceIndex - 1][targetIndex - 1].deleteScore;
        double prevInsertScore = objectScores[sourceIndex - 1][targetIndex - 1].insertScore;
        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            objectScores[sourceIndex][targetIndex].matchOriginOp = bestPreviousOperation;

            double bestPreviousOperationScore = minimumOperationScore(prevMatchScore, prevDeleteScore, prevInsertScore);
            return bestPreviousOperationScore + OctiLineSegment.match(objectScores[sourceIndex - 1][targetIndex - 1], sourceSegment, targetSegment);
        } catch (NoMinimumOperationException e) { // all three prev operations were impossible

            objectScores[sourceIndex][targetIndex].matchOriginOp = IMPOSSIBLE;
            return IMPOSSIBLE;
        }
    }

    /**
     * Calculates the score for the case that the OctiLineStrings end in the
     * points sourceIndex and targetIndex with a DELETE - operation
     *
     * @param sourceIndex the index of the source OctiLineSegment's endpoint
     * @param targetIndex the index of the target OctiLineSegment's endpoint
     * @return the calculated score
     */
    private double calcDeletionScore(int sourceIndex, int targetIndex) {
        OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(sourceIndex);
        OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(targetIndex);

        double prevMatchScore = objectScores[sourceIndex - 1][targetIndex].matchScore;
        double prevDeleteScore = objectScores[sourceIndex - 1][targetIndex].deleteScore;
        double prevInsertScore = objectScores[sourceIndex - 1][targetIndex].insertScore;

        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            objectScores[sourceIndex][targetIndex].deleteOriginOp = bestPreviousOperation;

            double bestPreviousOperationScore = minimumOperationScore(prevMatchScore, prevDeleteScore, prevInsertScore);
            return bestPreviousOperationScore + OctiLineSegment.deleteOnto(objectScores[sourceIndex - 1][targetIndex], sourceSegment, targetSegment.p1);
        } catch (NoMinimumOperationException e) { // all three prev operations were impossible
            objectScores[sourceIndex][targetIndex].deleteOriginOp = IMPOSSIBLE;
            // therefore no need to calc current operation
            return IMPOSSIBLE;
        }

    }

    /**
     * Calculates the score for the case that the OctiLineStrings end in the
     * points sourceIndex and targetIndex with a INSERT - operation
     *
     * @param sourceIndex the index of the source OctiLineSegment's endpoint
     * @param targetIndex the index of the target OctiLineSegment's endpoint
     * @return the calculated score
     */
    private double calcInsertionScore(int sourceIndex, int targetIndex) {
        double prevMatchScore = objectScores[sourceIndex][targetIndex - 1].matchScore;
        double prevDeleteScore = objectScores[sourceIndex][targetIndex - 1].deleteScore;
        double prevInsertScore = objectScores[sourceIndex][targetIndex - 1].insertScore;

        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            objectScores[sourceIndex][targetIndex].insertOriginOp = bestPreviousOperation;

            double bestPreviousOperationScore = minimumOperationScore(prevMatchScore, prevDeleteScore, prevInsertScore);

            OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(sourceIndex);
            OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(targetIndex);
            return bestPreviousOperationScore + OctiLineSegment.createFrom(objectScores[sourceIndex][targetIndex - 1], sourceSegment.p1, targetSegment);

        } catch (NoMinimumOperationException e) {// all three prev operations were impossible
            objectScores[sourceIndex][targetIndex].insertOriginOp = IMPOSSIBLE;

            // therefore no need to calc current operation
            return IMPOSSIBLE;
        }
    }

    /**
     * Helper function, returning the index (0-indexed) of the input parameter corresponding to the best Score
     * if Equal MATCH &lt; INSERT &lt; DELETE
     *
     * @param matchScore  score corresponding to MATCH (=0)
     * @param deleteScore score corresponding to DELETE (=1)
     * @param insertScore score corresponding to INSERT (=2)
     * @return index of the input parameter corresponding to the best Score
     * @throws NoMinimumOperationException if all 3 parameters are +inf
     */
    public int minimumOperation(double matchScore, double deleteScore, double insertScore) throws NoMinimumOperationException {
        if (deleteScore < matchScore) {
            if (deleteScore < insertScore) return DELETE;
            return INSERT;
        }
        if (insertScore < matchScore) return INSERT;
        if (matchScore == IMPOSSIBLE) throw new NoMinimumOperationException(); // all params are +inf
        return MATCH;
    }

    public double minimumOperationScore(double matchScore, double deleteScore, double insertScore) throws NoMinimumOperationException {
        if (deleteScore < matchScore) {
            if (deleteScore < insertScore) return deleteScore;
            return insertScore;
        }
        if (insertScore < matchScore) return insertScore;
        if (matchScore == IMPOSSIBLE) throw new NoMinimumOperationException();
        return matchScore;
    }

}
