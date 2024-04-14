package cz.pts.ptsworker.controller;

import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/exec")
public class ExecController {

    private final TestExecutionService testExecutionService;

    public ExecController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
    }

    @PostMapping
    public void executeTest(@RequestBody TestExecutionDto testExecutionDto) {
        testExecutionService.executeTest(testExecutionDto);
    }

    @GetMapping("/active")
    public Set<String> getActiveTestIds() {
        return testExecutionService.getActiveTestIds();
    }

    @DeleteMapping("/{testExecutionId}")
    public void terminateTest(@PathVariable(name = "testExecutionId") String executionId) {
        testExecutionService.terminateTestExecution(executionId);
    }

}
