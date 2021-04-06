package morph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MatrixElement {
    private static final Logger logger = LogManager.getLogger();
    double matchScore;
    double deleteScore;
    double insertScore;

    double matchOriginOp;
    double insertOriginOp;
    double deleteOriginOp;

    public int insertionsInARow = 0;
    public int deletionsInARow = 0;
    private MatrixElement bestPreviousElement;

    OctiSegmentAlignment.Operation minOperation;

    /** if equal : MATCH < INSERT < DELETE
     *
     * @param matchScore
     * @param deleteScore
     * @param insertScore
     */
    public void setMinimumOperation(double matchScore, double deleteScore, double insertScore){
        if(matchScore == Double.POSITIVE_INFINITY && deleteScore == Double.POSITIVE_INFINITY && insertScore == Double.POSITIVE_INFINITY){
            minOperation = null;
            return;
        }
        if (deleteScore < matchScore) {
            if(deleteScore < insertScore) {
                minOperation = OctiSegmentAlignment.Operation.Delete;
            }else{
                minOperation = OctiSegmentAlignment.Operation.Insert;
            }
        }else if (insertScore < matchScore){
            minOperation = OctiSegmentAlignment.Operation.Insert;
        }else{
            minOperation = OctiSegmentAlignment.Operation.Match;
        }
    }

    public void setMinimumOperation(){
        setMinimumOperation(matchScore,deleteScore,insertScore);
    }

    public void setPrevious(MatrixElement previous, OctiSegmentAlignment.Operation operation){
        this.bestPreviousElement = previous;
        this.minOperation = operation;
        if(previous != null && previous.minOperation == operation) {
            if(operation == OctiSegmentAlignment.Operation.Insert) {
                logger.warn("previous ins: " + previous.insertionsInARow);
                insertionsInARow = previous.insertionsInARow + 1;
                logger.warn("now: " + insertionsInARow);
            }
            if(operation == OctiSegmentAlignment.Operation.Delete){
                logger.warn("previous del: " + previous.deletionsInARow);
                deletionsInARow = previous.deletionsInARow + 1;
                logger.warn("now: " + deletionsInARow);
            }
        }
    }

    public MatrixElement getBestPreviousElement(){
        return bestPreviousElement;
    }
}
