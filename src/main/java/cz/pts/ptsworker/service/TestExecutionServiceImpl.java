package cz.pts.ptsworker.service;

import cz.pts.ptsworker.dto.ResultType;
import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;
import cz.pts.ptsworker.tailer.LogFileTailerListener;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;

@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    private final Map<String, TestRunHolder> testRunMap = Collections.synchronizedMap(new HashMap<>());
    private final TaskExecutor taskExecutor;
    private final RestTemplate restTemplate;

    // TODO tohle si sem budeme muset posílat nějak skrz property / config mapu....
    private static final String CONTROL_NODE_BASE_URL = "http://control:8084/";
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionServiceImpl.class);

    public TestExecutionServiceImpl(@Qualifier("testExecutor") TaskExecutor taskExecutor, RestTemplate restTemplate) {
        this.taskExecutor = taskExecutor;
        this.restTemplate = restTemplate;
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
        Assert.hasText(testExecutionDto.getLogFileName(), "logFileName cannot be empty");

        if (testRunMap.containsKey(testExecutionDto.getTestExecutionId())) {
            throw new IllegalArgumentException("TestExecutionId is already in use.");
        }

        File testFile = new File(testExecutionDto.getToolDirectoryPath() + "/" + testExecutionDto.getTestFileName());
        if (!testFile.exists()) {
            logger.error("Test file with name {} not found in directory {} ", testExecutionDto.getTestFileName(), testExecutionDto.getToolDirectoryPath());
            throw new IllegalArgumentException("Test file with name " + testExecutionDto.getTestFileName() + " not found in directory " + testExecutionDto.getToolDirectoryPath());
        }

        String finalLogFileName = testExecutionDto.getTestExecutionId() + "-" + testExecutionDto.getLogFileName();
        logger.info("Final log file name: {}", finalLogFileName);
        String command = composeCommand(testExecutionDto, finalLogFileName);
        logger.info("Trying to execute command: {}", command);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "/usr/bin/sh",
                "-c",
                command
        );
        pb.redirectErrorStream(true);

        taskExecutor.execute(() -> {
            Process p = null;
            try {
                logger.info("Starting new process.");
                p = pb.start();

                printStream(p);

                TestRunHolder testRunHolder = new TestRunHolder();
                testRunHolder.setProcess(p);
                testRunMap.put(testExecutionDto.getTestExecutionId(), testRunHolder);
                // TODO novy vlakno na posilani batchu...
                if(ResultType.LINE_BATCH.equals(testExecutionDto.getResultProcessingConfig().getResultType())) {
                    String dir = testExecutionDto.getToolDirectoryPath();
                    if(!testExecutionDto.getToolDirectoryPath().endsWith("/")) {
                        dir += "/";
                    }
                    sendResultsBatch(testRunHolder, testExecutionDto.getResultProcessingConfig().getBatchSize(), dir + finalLogFileName, testExecutionDto.getTestExecutionId());
                }

                logger.info("Wait for is called");
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
                // TODO send file to control node.
                if (ResultType.RESULTS_FILE.equals(testExecutionDto.getResultProcessingConfig().getResultType())) {
                    sendResultFile(testExecutionDto.getToolDirectoryPath(), finalLogFileName, testExecutionDto.getTestExecutionId());
                } else if (ResultType.LINE_BATCH.equals(testExecutionDto.getResultProcessingConfig().getResultType())) {
                    terminateBatchProcessing(testExecutionDto.getTestExecutionId());
                }
                // TODO send test end information???
            }
        });
    }

    private void terminateBatchProcessing(String testExecutionId) {
        TestRunHolder testRunHolder = testRunMap.get(testExecutionId);
        if(testRunHolder != null) {
            if(testRunHolder.getTailerListener() != null) {
                // send current lines in batch list
                testRunHolder.getTailerListener().flush();
            }
            if(testRunHolder.getTailer() != null) {
                testRunHolder.getTailer().close();
            }
        }
    }

    private void sendResultFile(String fileDir, String resultFileName, String testExecutionId) {
        String dir = fileDir.endsWith("/") ? fileDir : fileDir + "/";
        FileSystemResource resource = new FileSystemResource(new File(dir + resultFileName));
        MultiValueMap<String, Object> request = new LinkedMultiValueMap<>();
        request.add("results", resource);
        restTemplate.put(CONTROL_NODE_BASE_URL + "/api/test/result/file/" + testExecutionId, request);
        logger.info("Log file with results has been sent to control node.");
    }

    private String composeCommand(TestExecutionDto dto, String finalLogFileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(dto.getToolDirectoryPath());
        sb.append(" && ");
        sb.append(dto.getToolRunCommand());
        String cmd = sb.toString();
        return cmd.replace(dto.getLogFileName(), finalLogFileName);
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

    private void sendResultsBatch(TestRunHolder holder, int batchSize, String filePath, String testExecutionId) {
        // this has to run in separate thread, because the parent thread execution would be stopped on bufferedReader.readLine() method
        taskExecutor.execute(() -> {
            // 1 sniff file
            FileInputStream fs = null;
            // TODO logic with retries? - if the test never runs, this thread will hang here
            logger.info("Trying to find file on path: {}", filePath);

            while(fs == null){
                try {
                    fs = new FileInputStream(filePath);
                } catch (FileNotFoundException e){
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
            LogFileTailerListener tailerListener = new LogFileTailerListener(restTemplate, batchSize, CONTROL_NODE_BASE_URL, testExecutionId);
            holder.setTailerListener(tailerListener);
            logger.info("Tailing log file.");
            Tailer tailer = Tailer.builder().setFile(filePath).setStartThread(false).setTailerListener(tailerListener).get();
            holder.setTailer(tailer);
            taskExecutor.execute(tailer);
        });

    }


    /*
    private void sendResultsBatch(int batchSize, String filePath, String testExecutionId) {
        // this has to run in separate thread, because the parent thread execution would be stopped on bufferedReader.readLine() method
        // TODO nemusim pouzit tailer????
        taskExecutor.execute(() -> {
            List<String> lines = new ArrayList<>();
            String line;
            String controlLogFileName = null;

            try {
                logger.info("Reading log file {}", filePath);
                BufferedReader reader = new BufferedReader(new FileReader(filePath));
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    if (lines.size() == batchSize) {
                        Map<String, String> params = new HashMap<>();
                        if (controlLogFileName != null) {
                            logger.info("Setting logFileName param {}", controlLogFileName);
                            params.put("logFileName", controlLogFileName);
                        }
                        logger.info("Trying to send file batch");
                        ResponseEntity<String> response = restTemplate.exchange(CONTROL_NODE_BASE_URL + "/api/test/result/batch/" + testExecutionId, HttpMethod.PUT, new HttpEntity<>(lines), String.class, params);
                        controlLogFileName = response.getBody();
                        lines.clear();
                    }
                }

            logger.info("READING RESULTS FILE HAS ENDED");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }*/

}
