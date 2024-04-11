package cz.pts.ptsworker.dto;

public class ResultProcessingConfig {
    private int skipLines;
    private String skipPattern;

    private ResultType resultType = ResultType.RESULTS_FILE;

    // only define on ResultType.LINES_BATCH
    private int batchSize;

    public int getSkipLines() {
        return skipLines;
    }

    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

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
