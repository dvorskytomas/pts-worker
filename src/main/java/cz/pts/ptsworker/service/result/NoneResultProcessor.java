package cz.pts.ptsworker.service.result;

import cz.pts.ptsworker.dto.ResultType;
import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;
import cz.pts.ptsworker.util.TestExecutionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoneResultProcessor implements ResultProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(NoneResultProcessor.class);

    @Override
    public boolean shouldHandle(ResultType resultType) {
        return ResultType.NONE.equals(resultType);
    }

    @Override
    public TestExecutionDto validateAndChangeExecutionDefinition(TestExecutionDto testExecutionDto) {
        String finalCommand = TestExecutionUtils.composeCommand(testExecutionDto, testExecutionDto.getLogFileName());
        logger.info("Final command to execute: {}", finalCommand);
        testExecutionDto.setToolRunCommand(finalCommand);

        return testExecutionDto;
    }

    @Override
    public void onTestStart(TestRunHolder testRunHolder) {
        // do nothing
    }

    @Override
    public void onTestEnd(TestRunHolder testRunHolder) {
        // do nothing
    }
}
