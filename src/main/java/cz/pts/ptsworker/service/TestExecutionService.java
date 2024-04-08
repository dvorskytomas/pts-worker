package cz.pts.ptsworker.service;

import cz.pts.ptsworker.dto.TestExecutionDto;

import java.util.Set;

public interface TestExecutionService {

    void executeTest(TestExecutionDto testExecutionDto);

    void terminateTestExecution(String executionId);

    Set<String> getActiveTestIds();

}
