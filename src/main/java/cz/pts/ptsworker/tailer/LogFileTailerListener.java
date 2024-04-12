package cz.pts.ptsworker.tailer;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class LogFileTailerListener extends TailerListenerAdapter {

    private String controlLogFileName;

    private final RestTemplate restTemplate;
    private final int batchSize;
    private final String controlNodeBaseUrl;
    private final String testExecutionId;

    private final Integer workerNumber;
    private final List<String> lines = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(LogFileTailerListener.class);

    public LogFileTailerListener(RestTemplate restTemplate, int batchSize, String controlNodeBaseUrl, String testExecutionId, Integer workerNumber) {
        this.restTemplate = restTemplate;
        this.batchSize = batchSize;
        this.controlNodeBaseUrl = controlNodeBaseUrl;
        this.testExecutionId = testExecutionId;
        this.workerNumber = workerNumber;
    }

    public void setControlLogFileName(String controlLogFileName) {
        this.controlLogFileName = controlLogFileName;
    }

    public void flush() {
        sendBatchAndClear(true);
    }

    @Override
    public void handle(String line) {
        lines.add(line);
        if (lines.size() == batchSize) {
            sendBatchAndClear(false);
        }
    }

    private void sendBatchAndClear(boolean lastBatch) {
        String url = controlNodeBaseUrl + "/api/test/result/batch/" + testExecutionId;

        logger.info("Setting workerNumber param {}", workerNumber);
        url += "?workerNumber=" + workerNumber;
        url += "&lastBatch=" + lastBatch;

        if (controlLogFileName != null) {
            logger.info("Setting logFileName param {}", controlLogFileName);
            url += "&logFileName=" + controlLogFileName;
        }
        logger.info("Trying to send file batch");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(lines), String.class);
        this.setControlLogFileName(response.getBody());
        lines.clear();
    }

}
