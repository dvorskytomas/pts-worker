package cz.pts.ptsworker.service;

import cz.pts.ptsworker.dto.TestExecutionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    private final TaskExecutor taskExecutor;
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionServiceImpl.class);

    public TestExecutionServiceImpl(@Qualifier("testExecutor") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void executeTest(TestExecutionDto testExecutionDto) {
        logger.info("Trying to execute new test run.");

        // "cd /opt/jmeter/bin && ./jmeter -n -t firstTest.jmx -l results.jtl"
        String command = composeCommand(testExecutionDto);
        logger.info("Trying to execute command: {}", command);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "/usr/bin/sh",
                "-c",
                command
        );
        pb.redirectErrorStream(true);

        taskExecutor.execute(() -> {
            Process p;
            try {
                logger.info("Starting new process.");
                p = pb.start();

                printStream(p.getInputStream());

            } catch (IOException e) {
                logger.error("Error starting new process", e);
                throw new RuntimeException(e);
            }
        });
    }

    private String composeCommand(TestExecutionDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(dto.getToolDirectoryPath());
        sb.append(" && ");
        sb.append(dto.getToolRunCommand());
        return sb.toString();
    }

    private void printStream(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream)
        )) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logger.info(line);
            }
        }
    }
}
