package cz.pts.ptsworker.dto;

public class ResultProcessingConfig {
    private String skipPattern;

    private ResultType resultType = ResultType.RESULTS_FILE;

    // only define on ResultType.LINES_BATCH
    private int batchSize;

    public String getSkipPattern() {
        return skipPattern;
    }

    public void setSkipPattern(String skipPattern) {
        this.skipPattern = skipPattern;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
