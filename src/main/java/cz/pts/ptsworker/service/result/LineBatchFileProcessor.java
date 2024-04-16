package cz.pts.ptsworker.service.result;

import cz.pts.ptsworker.dto.ResultType;
import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;
import cz.pts.ptsworker.tailer.LogFileTailerListener;
import cz.pts.ptsworker.util.TestExecutionUtils;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Component
public class LineBatchFileProcessor implements ResultProcessorService {

    @Value("${control.base.url}")
    private String controlNodeBaseUrl;

    private final TaskExecutor taskExecutor;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(LineBatchFileProcessor.class);

    public LineBatchFileProcessor(@Qualifier("testExecutor") TaskExecutor taskExecutor, RestTemplate restTemplate) {
        this.taskExecutor = taskExecutor;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean shouldHandle(ResultType resultType) {
        return ResultType.LINE_BATCH_FILE.equals(resultType);
    }

    @Override
    public TestExecutionDto validateAndChangeExecutionDefinition(TestExecutionDto testExecutionDto) {
        Assert.hasText(testExecutionDto.getLogFileName(), "logFileName cannot be empty.");
        Assert.isTrue(testExecutionDto.getResultProcessingConfig().getBatchSize() > 0, "batchSize cannot be less than 1.");

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
        taskExecutor.execute(() -> {
            // 1 sniff file
            FileInputStream fs = null;
            // TODO logic with retries?
            logger.info("Trying to find file on path: {}", testRunHolder.getLogFilePath());

            while (fs == null) {
                try {
                    fs = new FileInputStream(testRunHolder.getLogFilePath());
                } catch (FileNotFoundException e) {
                    logger.warn("File not found: " + e.getMessage());
                }
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            logger.info("File found!!!");
            try {
                fs.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 2 tail file
            LogFileTailerListener tailerListener = new LogFileTailerListener(restTemplate, testRunHolder.getTestExecutionDto().getResultProcessingConfig().getBatchSize(), controlNodeBaseUrl, testRunHolder.getTestExecutionDto().getTestExecutionId(), testRunHolder.getTestExecutionDto().getWorkerNumber());
            testRunHolder.setTailerListener(tailerListener);
            logger.info("Tailing log file.");
            Tailer tailer = Tailer.builder().setFile(testRunHolder.getLogFilePath()).setStartThread(false).setTailerListener(tailerListener).get();
            testRunHolder.setTailer(tailer);
            taskExecutor.execute(tailer);
        });
    }

    @Override
    public void onTestEnd(TestRunHolder testRunHolder) {
        logger.info("On test end called.");
        if (testRunHolder.getTailerListener() != null) {
            // send current lines in batch list
            testRunHolder.getTailerListener().flush();
        }
        if (testRunHolder.getTailer() != null) {
            testRunHolder.getTailer().close();
        }
    }
}
