package cz.pts.ptsworker.dto;

public class TestExecutionDto {

    private String testExecutionId;
    private String toolDirectoryPath;
    private String toolRunCommand;
    private String logFileName;
    private String testFileName;
    private ResultProcessingConfig resultProcessingConfig;

    public String getTestExecutionId() {
        return testExecutionId;
    }

    public void setTestExecutionId(String testExecutionId) {
        this.testExecutionId = testExecutionId;
    }

    public String getToolDirectoryPath() {
        return toolDirectoryPath;
    }

    public void setToolDirectoryPath(String toolDirectoryPath) {
        this.toolDirectoryPath = toolDirectoryPath;
    }

    public String getToolRunCommand() {
        return toolRunCommand;
    }

    public void setToolRunCommand(String toolRunCommand) {
        this.toolRunCommand = toolRunCommand;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public String getTestFileName() {
        return testFileName;
    }

    public void setTestFileName(String testFileName) {
        this.testFileName = testFileName;
    }

    public ResultProcessingConfig getResultProcessingConfig() {
        return resultProcessingConfig;
    }

    public void setResultProcessingConfig(ResultProcessingConfig resultProcessingConfig) {
        this.resultProcessingConfig = resultProcessingConfig;
    }
}
