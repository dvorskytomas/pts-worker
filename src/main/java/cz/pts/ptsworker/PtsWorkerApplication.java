package cz.pts.ptsworker;

import cz.pts.ptsworker.config.PtsWorkerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(PtsWorkerConfig.class)
@SpringBootApplication
public class PtsWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PtsWorkerApplication.class, args);
	}

}
