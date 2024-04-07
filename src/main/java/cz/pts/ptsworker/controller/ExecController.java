package cz.pts.ptsworker.controller;

import cz.pts.ptsworker.dto.TestExecutionDto;
import cz.pts.ptsworker.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

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

    /*
    @PostMapping("/jmeter")
    public void executeTestJmeter(){
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "/usr/bin/sh",
                "-c",
                "cd /opt/jmeter/bin && ./jmeter -n -t firstTest.jmx -l results.jtl"
        );
        pb.redirectErrorStream(true);

        Thread t = new Thread(() -> {
            Process p;
            try {
                System.out.println("Starting process builder in different thread.");
                p = pb.start();

                printStream(p.getInputStream());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        t.start();
    }

    @PostMapping("/k6")
    public void executeTestk6(){
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "/usr/bin/sh",
                "-c",
                "cd /opt/k6 && ./k6 run --vus 1 --duration 30s firstTest.js --out csv=results.csv"
        );
        pb.redirectErrorStream(true);

        Thread t = new Thread(() -> {
            Process p;
            try {
                System.out.println("Starting process builder in different thread.");
                p = pb.start();

                printStream(p.getInputStream());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        t.start();
    }

    private void printStream(InputStream inputStream) throws IOException {
        try(BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream)
        )) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }

        }
    }*/

}
