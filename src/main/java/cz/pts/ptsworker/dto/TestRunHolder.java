package cz.pts.ptsworker.dto;

import cz.pts.ptsworker.tailer.LogFileTailerListener;
import org.apache.commons.io.input.Tailer;

public class TestRunHolder {

    private Process process;
    private Tailer tailer;
    private LogFileTailerListener tailerListener;
    private TestExecutionDto testExecutionDto;
    private String logFilePath;

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Tailer getTailer() {
        return tailer;
    }

    public void setTailer(Tailer tailer) {
        this.tailer = tailer;
    }

    public LogFileTailerListener getTailerListener() {
        return tailerListener;
    }

    public void setTailerListener(LogFileTailerListener tailerListener) {
        this.tailerListener = tailerListener;
    }

    public TestExecutionDto getTestExecutionDto() {
        return testExecutionDto;
    }

    public void setTestExecutionDto(TestExecutionDto testExecutionDto) {
        this.testExecutionDto = testExecutionDto;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
}
