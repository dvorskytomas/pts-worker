package cz.pts.ptsworker.service.result;

import cz.pts.ptsworker.dto.ResultType;
import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.dto.TestRunHolder;

public interface ResultProcessorService {

    boolean shouldHandle(ResultType resultType);

    TestExecutionDto validateAndChangeExecutionDefinition(TestExecutionDto testExecutionDto);

    void onTestStart(TestRunHolder testRunHolder);

    void onTestEnd(TestRunHolder testRunHolder);
}
