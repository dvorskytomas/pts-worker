package cz.pts.ptsworker;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/api/exec")
public class ExecController {

    @PostMapping
    public void executeTest(){
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
    }

}
