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
import scoringStrategies.BaseMatchStrategy;
import scoringStrategies.VisibilityMatchStrategy;

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

    private double[][][] scores;

    public static void main(String[] args) {
        String svgPath;
        if(args.length == 1){ //interpret arg as path
            svgPath = args[0];
        }else{
            svgPath = "src/main/resources/squareTriangle.svg";
        }
        PolygonExtractorInterface extractor = new PolygonExtractor();
        try {
            extractor.parseFile(Paths.get(svgPath).toUri());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FileParseException e){
            e.printStackTrace();
            logger.warn("couldnt parse file content");
        }

        Geometry srcGeom = extractor.getNthGeometry(0); //ogf.createOctiLineString(importer.getNthOctiLineString(0).getExteriorRing().getCoordinateSequence());
        Geometry tarGeom = extractor.getNthGeometry(1); //ogf.createOctiLineString(importer.getNthOctiLineString(1).getExteriorRing().getCoordinateSequence());

        OctiLineString src, tar;
        try{
            src = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(srcGeom);
            tar = OctiGeometryFactory.OCTI_FACTORY.convertToOctiLineString(tarGeom);

        }catch (Exception e){
            e.printStackTrace();
            logger.warn("no valid Geometry to extract an OctiLineString from");
            return;
        }

        OctiLineSegment.setStrategy(new VisibilityMatchStrategy(new BaseMatchStrategy()), src, tar);
        OctiLineMatcher matcher = new OctiLineMatcher(src, tar);

        try {
            matcher.backtrack();
        } catch (NoMinimumOperationException e) {
            e.printStackTrace();
            logger.warn("no valid match");
        }
    }

    /** Brings the OctiLineStrings in a configuration, such that the algorithm can work on it.
     *
     * Assumptions:
     * <ul>
     *     <li>OctiLineStrings are closed</li>
     *     <li>OctiLineStrings do not self-intersect</li>
     * </ul>
     *
     * Config guaranties:
     *
     *  <ul>
     *      <li>if {@link OctiLineMatcher#sourceAndTargetSwapped} is <i>true</i> the strings will be swapped before returning</li>
     *      <li>OctiLineStrings are in clockwise orientation</li>
     *      <li>OctiLineStrings are rotated such that the starting points are the points returned by {@link OctiLineMatcher#determineBestStartingPoint}</li>
     *  </ul>
     *
     * @param src
     * @param tar
     * @returns the pair of configured {@link OctiLineString}s,
     * {@link Pair#first()} being the source,
     * {@link Pair#second()} being the target,
     */
    private Pair<OctiLineString, OctiLineString> createBaseConfig(OctiLineString src, OctiLineString tar){

        //checks for self-intersect, strings may be non-closed though
        if(! src.isSimple() ) throw new IllegalArgumentException("src input not simple");
        if(! tar.isSimple() ) throw new IllegalArgumentException("tar input not simple");

        if(sourceAndTargetSwapped){
            OctiLineString tempSource = src;
            src = tar;
            tar = tempSource;
            logger.trace("swapped source and target");
        }

        if(src.isClosed() && tar.isClosed()){
            if(Orientation.isCCW(src.getCoordinateSequence())){
                logger.trace("source is oriented in counter-clockwise order --> making it clockwise");
                src = (OctiLineString) src.reverse();
            }
            if(Orientation.isCCW(tar.getCoordinateSequence())){
                logger.trace("target is oriented in counter-clockwise order --> making it clockwise");
                tar = (OctiLineString) tar.reverse();
            }
        }

        //considers the case of non-closednes
        int[] startpoints = determineBestStartingPoint(src,tar);
        src = src.makeNthPointTheFirst(startpoints[0]);
        tar = tar.makeNthPointTheFirst(startpoints[1]);

        return new Pair<>(src,tar);
    }

    /**
     * Creates a new instance with the specified OctiLineStrings
     * @param sourceLineString the source String
     * @param targetLineString the target String
     */
    public OctiLineMatcher(OctiLineString sourceLineString, OctiLineString targetLineString) {
        Pair<OctiLineString,OctiLineString> baseConfig = createBaseConfig(sourceLineString,targetLineString);
        source = baseConfig.first();
        target = baseConfig.second();

        //must be after bringing the strings into start configuration
        OctiLineSegment.setStrategy(new BaseMatchStrategy(), source, target);

        initBoard();
        iterateBoard();

    }

    /**
     * Determines the starting Points of the alignment by selecting the two points (one from each LineString) with minimum distance to each other
     * @param first the first LineString
     * @param second the second LineString
     * @returns int array of size 2  with the preferred starting points,
     *          with the first element corresponding to the index of first,
     *          second element corresponding to the index of second
     */
    private int[] determineBestStartingPoint(LineString first, LineString second) {
        if(!first.isClosed() || !second.isClosed()){
            logger.warn("at least one linestring is not closed, returning starting points");
            return new int[]{0,0};
        }
        logger.info("determining starting point by minimal distance");
        int[] minIndices = {0, 0};
        double minDistance = first.getCoordinateN(0).distance(second.getCoordinateN(0));

        for (int i = 0; i < first.getNumPoints(); i++) {
            Coordinate from = first.getCoordinateN(i);
            for (int j = 0; j < second.getNumPoints(); j++) {
                Coordinate to = second.getCoordinateN(j);
                if (from.distance(to) < minDistance) {
                    minDistance = from.distance(to);
                    minIndices[0] = i;
                    minIndices[1] = j;
                }
            }
        }
        logger.debug("Min distance are points are: " + minIndices[0] + " (source) and " + minIndices[1] + " (target)");
        return minIndices;
    }

    public OctiLineString getSource() {
        return source;
    }

    public OctiLineString getTarget() {
        return target;
    }

    /** Intended to be called after the alignment was calculated, to give insight into the quality of the alignment
     *
     * @return the score the calculated Alignment was valued with, the lower the better
     * @throws NoMinimumOperationException in case no valid Alignment could be found
     */
    public double getEndScore() throws NoMinimumOperationException {
        int bestOperation = 0;
        try {
            bestOperation = minimumOperation(scores[source.size()][target.size()][MATCH],
                    scores[source.size()][target.size()][DELETE],
                    scores[source.size()][target.size()][INSERT]);
        } catch (NoMinimumOperationException e) {
            logger.info("No valid match possible");
            throw e;
        }

        logger.info("best match Score: " + scores[source.size()][target.size()][bestOperation]);
        return scores[source.size()][target.size()][bestOperation];
    }

    /** Backtracks the Double array to determine which path led to the best score
     *
     * @return a pair made of (Source-Index, Target-Index) , wrapped in another Pair with the second Part containing the operation,
     *          that led to the specified index position
     * @throws NoMinimumOperationException in case no valid Alignment could be found
     */
    private List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> backtrack() throws NoMinimumOperationException {
        List<Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation>> moves = new ArrayList<>();

        int sourceIndex = source.size();
        int targetIndex = target.size();

        int opIndex;
        try {
            opIndex = minimumOperation(
                    scores[sourceIndex][targetIndex][MATCH],
                    scores[sourceIndex][targetIndex][DELETE],
                    scores[sourceIndex][targetIndex][INSERT]);
        } catch (NoMinimumOperationException e) {
            logger.warn("could not backtrack, no valid match found");
            throw e;
        }
        OctiSegmentAlignment.Operation op = translate(opIndex);

        Pair<Integer, Integer> position = new Pair<>(sourceIndex, targetIndex);
        Pair<Pair<Integer, Integer>, OctiSegmentAlignment.Operation> move = new Pair<>(position, op);
        moves.add(move);
        //todo rework logic block to be more understandable
        while ((sourceIndex != 0 || targetIndex != 0)) {
            switch (opIndex) {
                case MATCH:
                    opIndex = (int) scores[sourceIndex][targetIndex][opIndex + 3];  // i was match, and to get here OP was used
                    sourceIndex -= 1;
                    targetIndex -= 1;
                    break;
                case DELETE:
                    opIndex = (int) scores[sourceIndex][targetIndex][opIndex + 3];
                    sourceIndex -= 1;
                    break;
                case INSERT:
                    opIndex = (int) scores[sourceIndex][targetIndex][opIndex + 3];
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
            logger.trace("To get to (" + m.first().first() + "," + m.first().second()+") -> " + m.second().name());
        }
        return moves;
    }

    /**
     * Helper function, maps 0->Match, 1->Delete, 2->Insert
     * @param code the integer Code
     * @return the Corresponding Operation
     */
    private OctiSegmentAlignment.Operation translate(int code){
        switch (code){
            case MATCH: return OctiSegmentAlignment.Operation.Match;
            case DELETE: return OctiSegmentAlignment.Operation.Delete;
            case INSERT: return OctiSegmentAlignment.Operation.Insert;
        }
        logger.error("invalid code");
        return null;
    }

    /** Queries for the OctiStringAlignment - Object in which the calculated alignment is encapsulated
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

        return new ConcreteOctiStringAlignment(source,target,alignmentIndicies);
    }
    /**
     * Inits the starting point (0 0) , aswell as the 0-th row and the 0-th column.
     * These only consist of insertion chains (i.e. columns) and  deletion chains (i.e. rows)
     */
    private void initBoard() {
        logger.info("init Board");
        scores = new double[source.size() + 1][target.size() + 1][6];

        scores[0][0][MATCH] = 0.0; //arbitrarily set to 0, must only conform to < IMPOSSIBLE
        scores[0][0][INSERT] = IMPOSSIBLE;
        scores[0][0][DELETE] = IMPOSSIBLE;

        //there's no origin for the origin
        scores[0][0][MATCH_ORIGIN_OP] = IMPOSSIBLE;
        scores[0][0][DELETE_ORIGIN_OP] = IMPOSSIBLE;
        scores[0][0][INSERT_ORIGIN_OP] = IMPOSSIBLE;

        for (int i = 1; i <= source.size(); i++) {
            scores[i][0][MATCH] = IMPOSSIBLE;
            scores[i][0][INSERT] = IMPOSSIBLE;
            scores[i][0][MATCH_ORIGIN_OP] = IMPOSSIBLE;
            scores[i][0][INSERT_ORIGIN_OP] = IMPOSSIBLE;

            // calc DELETE costs
            OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(i);
            Coordinate targetStartPoint = target.getCoordinateN(0);
            if (i == 1) {
                scores[i][0][DELETE] = OctiLineSegment.deleteOnto(sourceSegment, targetStartPoint);
                scores[i][0][DELETE_ORIGIN_OP] = MATCH;
            } else {
                //origin can only be a delete operation
                scores[i][0][DELETE] = scores[i - 1][0][DELETE] + OctiLineSegment.deleteOnto(sourceSegment, targetStartPoint);
                //make backtracking possible only if the previous deletion wasn't already impossible
                scores[i][0][DELETE_ORIGIN_OP] = (scores[i - 1][0][DELETE] == IMPOSSIBLE) ? IMPOSSIBLE : DELETE;

            }

        }
        for (int j = 1; j <= target.size(); j++) {
            scores[0][j][MATCH] = IMPOSSIBLE;
            scores[0][j][DELETE] = IMPOSSIBLE;
            scores[0][j][MATCH_ORIGIN_OP] = IMPOSSIBLE;
            scores[0][j][DELETE_ORIGIN_OP] = IMPOSSIBLE;

            // calc INSERT costs
            OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(j);
            Coordinate sourceStartPoint = source.getCoordinateN(0);

            if (j == 1) {
                scores[0][j][INSERT] = OctiLineSegment.createFrom(sourceStartPoint, targetSegment);
                scores[0][j][INSERT_ORIGIN_OP] = MATCH;
            } else {
                //origin can only be an insert operation
                scores[0][j][INSERT] = scores[0][j - 1][INSERT] + OctiLineSegment.createFrom(sourceStartPoint, targetSegment);
                //make backtracking possible only if the previous insertion wasn't already impossible
                scores[0][j][INSERT_ORIGIN_OP] = (scores[0][j - 1][INSERT] == IMPOSSIBLE) ? IMPOSSIBLE : INSERT;
            }
        }
    }

    /**
     * Iterates the board filling in all values.
     * Rows and columns are filled index-increasing order, origination from the diagonal lines elements
     */
    private void iterateBoard() {
        logger.info("iterating board");
        int smallerDiag = Math.min(scores.length, scores[0].length);
        for (int diagonalIndex = 1; diagonalIndex < smallerDiag; diagonalIndex++) {

            //diagonal elements
            logger.trace("calculating (" + diagonalIndex + ", " + diagonalIndex + ")");
            scores[diagonalIndex][diagonalIndex][MATCH] = calcMatchingScore(diagonalIndex, diagonalIndex);
            scores[diagonalIndex][diagonalIndex][DELETE] = calcDeletionScore(diagonalIndex, diagonalIndex);
            scores[diagonalIndex][diagonalIndex][INSERT] = calcInsertionScore(diagonalIndex, diagonalIndex);


            //fill rows starting from diagonal line
            for (int columnIndex = diagonalIndex + 1; columnIndex < scores.length; columnIndex++) {
                logger.trace("calculating (" + columnIndex + ", " + diagonalIndex + ")");
                scores[columnIndex][diagonalIndex][MATCH] = calcMatchingScore(columnIndex, diagonalIndex);
                scores[columnIndex][diagonalIndex][DELETE] = calcDeletionScore(columnIndex, diagonalIndex);
                scores[columnIndex][diagonalIndex][INSERT] = calcInsertionScore(columnIndex, diagonalIndex);
            }
            //fill colums starting from diagonal line
            for (int rowIndex = diagonalIndex + 1; rowIndex < scores[0].length; rowIndex++) {
                logger.trace("calculating (" + diagonalIndex + ", " + rowIndex + ")");
                scores[diagonalIndex][rowIndex][MATCH] = calcMatchingScore(diagonalIndex, rowIndex);
                scores[diagonalIndex][rowIndex][DELETE] = calcDeletionScore(diagonalIndex, rowIndex);
                scores[diagonalIndex][rowIndex][INSERT] = calcInsertionScore(diagonalIndex, rowIndex);
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
        if (sourceOrientaion != targetOrientaion) return IMPOSSIBLE; // todo factor out into strategy

        double prevMatchScore = scores[sourceIndex - 1][targetIndex - 1][MATCH];
        double prevDeleteScore = scores[sourceIndex - 1][targetIndex - 1][DELETE];
        double prevInsertScore = scores[sourceIndex - 1][targetIndex - 1][INSERT];

        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            scores[sourceIndex][targetIndex][MATCH_ORIGIN_OP] = bestPreviousOperation;

            double bestPreviousOperationScore = scores[sourceIndex - 1][targetIndex - 1][bestPreviousOperation];
            return bestPreviousOperationScore + OctiLineSegment.match(sourceSegment, targetSegment);
        } catch (NoMinimumOperationException e) { // all three prev operations were impossible

            scores[sourceIndex][targetIndex][MATCH_ORIGIN_OP] = IMPOSSIBLE;
            // therefore no need to calc current operation
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
        double prevMatchScore = scores[sourceIndex - 1][targetIndex][MATCH];
        double prevDeleteScore = scores[sourceIndex - 1][targetIndex][DELETE];
        double prevInsertScore = scores[sourceIndex - 1][targetIndex][INSERT];

        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            scores[sourceIndex][targetIndex][DELETE_ORIGIN_OP] = bestPreviousOperation;

            double bestPreviousOperationScore = scores[sourceIndex - 1][targetIndex][bestPreviousOperation];

            OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(sourceIndex);
            OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(targetIndex);
            return bestPreviousOperationScore + OctiLineSegment.deleteOnto(sourceSegment, targetSegment.p1);

        } catch (NoMinimumOperationException e) { // all three prev operations were impossible
            scores[sourceIndex][targetIndex][DELETE_ORIGIN_OP] = IMPOSSIBLE;

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
        double prevMatchScore = scores[sourceIndex][targetIndex - 1][MATCH];
        double prevDeleteScore = scores[sourceIndex][targetIndex - 1][DELETE];
        double prevInsertScore = scores[sourceIndex][targetIndex - 1][INSERT];

        try {
            int bestPreviousOperation = minimumOperation(prevMatchScore, prevDeleteScore, prevInsertScore);
            scores[sourceIndex][targetIndex][INSERT_ORIGIN_OP] = bestPreviousOperation;

            double bestPreviousOperationScore = scores[sourceIndex][targetIndex - 1][bestPreviousOperation];

            OctiLineSegment sourceSegment = source.getSegmentBeforeNthPoint(sourceIndex);
            OctiLineSegment targetSegment = target.getSegmentBeforeNthPoint(targetIndex);
            return bestPreviousOperationScore + OctiLineSegment.createFrom(sourceSegment.p1, targetSegment);

        } catch (NoMinimumOperationException e) {// all three prev operations were impossible
            scores[sourceIndex][targetIndex][INSERT_ORIGIN_OP] = IMPOSSIBLE;

            // therefore no need to calc current operation
            return IMPOSSIBLE;
        }
    }

    /**
     * Helper function, returning the index (0-indexed) of the input parameter corresponding to the best Score
     *
     * @param matchScore  score corresponding to MATCH (=0)
     * @param deleteScore score corresponding to DELETE (=1)
     * @param insertScore score corresponding to INSERT (=2)
     * @return index of the input parameter corresponding to the best Score
     */
    public int minimumOperation(double matchScore, double deleteScore, double insertScore) throws NoMinimumOperationException {
        if (deleteScore < matchScore) {
            if (insertScore < deleteScore) return INSERT;
            return DELETE;
        }
        if (insertScore < matchScore) return INSERT;
        if (matchScore == Double.POSITIVE_INFINITY) throw new NoMinimumOperationException(); // all params are +inf
        return MATCH;
    }

}
