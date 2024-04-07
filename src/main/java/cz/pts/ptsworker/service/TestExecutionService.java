package cz.pts.ptsworker.service;

import cz.pts.ptsworker.dto.TestExecutionDto;

public interface TestExecutionService {

    void executeTest(TestExecutionDto testExecutionDto);

}
