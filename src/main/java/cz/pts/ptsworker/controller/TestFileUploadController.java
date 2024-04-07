package cz.pts.ptsworker.controller;

import cz.pts.ptsworker.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class TestFileUploadController {

    private final FileUploadService fileUploadService;
    private static final Logger logger = LoggerFactory.getLogger(TestFileUploadController.class);
    public TestFileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadTestFile(@RequestPart(value = "file") MultipartFile file, @RequestPart(value = "destinationFolder") String destination) {
        logger.info(destination);
        fileUploadService.uploadFile(file, destination);
    }

}
