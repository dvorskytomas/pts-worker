package cz.pts.ptsworker.service.result;

import cz.pts.ptsworker.dto.ResultType;
import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;
import cz.pts.ptsworker.util.TestExecutionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Component
public class ResultsFileProcessor implements ResultProcessorService {

    @Value("${control.base.url}")
    private String controlNodeBaseUrl;

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ResultsFileProcessor.class);

    public ResultsFileProcessor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean shouldHandle(ResultType resultType) {
        return ResultType.RESULTS_FILE.equals(resultType);
    }

    @Override
    public TestExecutionDto validateAndChangeExecutionDefinition(TestExecutionDto testExecutionDto) {
        Assert.hasText(testExecutionDto.getLogFileName(), "logFileName cannot be empty");

        String finalLogFileName = TestExecutionUtils.composeFinalLogFileName(testExecutionDto);
        String finalCommand = TestExecutionUtils.composeCommand(testExecutionDto, finalLogFileName);

        logger.info("Final log file name: {}", finalLogFileName);
        logger.info("Final command to execute: {}", finalCommand);

        testExecutionDto.setLogFileName(finalLogFileName);
        testExecutionDto.setToolRunCommand(finalCommand);

        return testExecutionDto;
    }

    @Override
    public void onTestStart(TestRunHolder testRunHolder) {
        // do nothing
    }

    @Override
    public void onTestEnd(TestRunHolder testRunHolder) {
        FileSystemResource resource = new FileSystemResource(new File(testRunHolder.getLogFilePath()));
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("results", resource);
        restTemplate.put(controlNodeBaseUrl + "/api/test/result/file/" + testRunHolder.getTestExecutionDto().getTestExecutionId() + "?workerNumber=" + testRunHolder.getTestExecutionDto().getWorkerNumber(), request);
        logger.info("Log file with results has been sent to control node.");
    }
}
