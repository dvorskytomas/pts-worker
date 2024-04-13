package cz.pts.ptsworker.service;


import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;
import cz.pts.ptsworker.service.result.ResultProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;

@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    private final Map<String, TestRunHolder> testRunMap = Collections.synchronizedMap(new HashMap<>());
    private final TaskExecutor taskExecutor;
    private final RestTemplate restTemplate;
    private final List<ResultProcessorService> resultProcessorServices;

    // TODO tohle si sem budeme muset posílat nějak skrz property / config mapu....
    private static final String CONTROL_NODE_BASE_URL = "http://control:8084/";
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionServiceImpl.class);

    public TestExecutionServiceImpl(@Qualifier("testExecutor") TaskExecutor taskExecutor,
                                    RestTemplate restTemplate,
                                    List<ResultProcessorService> resultProcessorServices) {
        this.taskExecutor = taskExecutor;
        this.restTemplate = restTemplate;
        this.resultProcessorServices = resultProcessorServices;
    }

    @Override
    public Set<String> getActiveTestIds() {
        return testRunMap.keySet();
    }

    @Override
    public void terminateTestExecution(String executionId) {
        TestRunHolder testRunHolder = testRunMap.get(executionId);
        if (testRunHolder != null && testRunHolder.getProcess() != null) {
            logger.warn("Cancelling test process forcibly!");
            testRunHolder.getProcess().destroyForcibly();

            terminateBatchProcessing(executionId);

            testRunMap.remove(executionId);
        } else {
            logger.error("ExecutionId {} not found.", executionId);
            throw new IllegalArgumentException("Test process with executionId: " + executionId + " was not found.");
        }
    }

    @Override
    public void executeTest(TestExecutionDto testExecutionDto) {
        logger.info("Trying to execute new test run.");
        Assert.notNull(testExecutionDto.getTestExecutionId(), "testExecutionId cannot be null");
        Assert.notNull(testExecutionDto.getWorkerNumber(), "Worker number cannot be null");
        Assert.notNull(testExecutionDto.getResultProcessingConfig(), "resultProcessingConfig cannot be null");

        // unique id check
        if (testRunMap.containsKey(testExecutionDto.getTestExecutionId())) {
            throw new IllegalArgumentException("TestExecutionId is already in use.");
        }

        // test file existence check
        File testFile = new File(testExecutionDto.getToolDirectoryPath() + "/" + testExecutionDto.getTestFileName());
        if (!testFile.exists()) {
            logger.error("Test file with name {} not found in directory {} ", testExecutionDto.getTestFileName(), testExecutionDto.getToolDirectoryPath());
            throw new IllegalArgumentException("Test file with name " + testExecutionDto.getTestFileName() + " not found in directory " + testExecutionDto.getToolDirectoryPath());
        }
        // select service
        ResultProcessorService resultProcessor = resultProcessorServices.stream()
                .filter(resultProcessorService -> resultProcessorService.shouldHandle(testExecutionDto.getResultProcessingConfig().getResultType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No processorService has been resolved."));

        // validate needed attributes and change definition e.g. logFileName
        TestExecutionDto finalTestExecutionDto = resultProcessor.validateAndChangeExecutionDefinition(testExecutionDto);

        // initialize holder
        TestRunHolder testRunHolder = new TestRunHolder();
        testRunHolder.setTestExecutionDto(finalTestExecutionDto);
        testRunHolder.setLogFilePath(composeLogFilePath(finalTestExecutionDto.getLogFileName(), finalTestExecutionDto.getToolDirectoryPath()));

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "/usr/bin/sh",
                "-c",
                finalTestExecutionDto.getToolRunCommand()
        );
        pb.redirectErrorStream(true);

        taskExecutor.execute(() -> {
            Process p = null;
            try {
                logger.info("Starting new process.");
                p = pb.start();

                printStream(p);

                testRunHolder.setProcess(p);
                testRunMap.put(finalTestExecutionDto.getTestExecutionId(), testRunHolder);

                resultProcessor.onTestStart(testRunHolder);

                logger.info("Waiting for process to finish.");
                p.waitFor();

            } catch (IOException e) {
                logger.error("Error starting new process", e);
                throw new IllegalStateException(e);
            } catch (InterruptedException e) {
                logger.error("Process thread has been interrupted.", e);
                throw new IllegalStateException(e);
            } finally {
                logger.info("Test execution has been stopped.");
                if (p != null && p.isAlive()) {
                    logger.info("Gracefully ending test execution.");
                    p.destroy();
                }

                resultProcessor.onTestEnd(testRunHolder);
                // testEnd + cleanup
                String testEndUrl = CONTROL_NODE_BASE_URL + "/api/test/end/{testExecutionId}?workerNumber={workerNumber}";
                restTemplate.postForObject(testEndUrl, null, Void.class, finalTestExecutionDto.getTestExecutionId(), finalTestExecutionDto.getWorkerNumber());
                testRunMap.remove(finalTestExecutionDto.getTestExecutionId());
            }
        });
    }

    private String composeLogFilePath(String logFileName, String logFileDir) {
        String dir = logFileDir;
        if (!logFileDir.endsWith("/")) {
            dir += "/";
        }
        return dir + logFileName;
    }

    private void terminateBatchProcessing(String testExecutionId) {
        TestRunHolder testRunHolder = testRunMap.get(testExecutionId);
        if (testRunHolder != null) {
            if (testRunHolder.getTailerListener() != null) {
                // send current lines in batch list
                testRunHolder.getTailerListener().flush();
            }
            if (testRunHolder.getTailer() != null) {
                testRunHolder.getTailer().close();
            }
        }
    }

    private void printStream(Process process) {
        // this has to run in separate thread, because the parent thread execution would be stopped on bufferedReader.readLine() method
        taskExecutor.execute(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while (process.isAlive() && (line = bufferedReader.readLine()) != null) {
                    logger.info(line);
                }
            } catch (IOException e) {
                logger.error("Exception when logging process log.", e);
            }
        });

    }

}
