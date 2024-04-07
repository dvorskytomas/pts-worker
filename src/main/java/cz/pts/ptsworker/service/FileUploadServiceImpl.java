package cz.pts.ptsworker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    @Override
    public void uploadFile(MultipartFile multipart, String destination) {
        File testFile = new File(destination + "/" + multipart.getOriginalFilename());

        logger.info("Saving file {}", testFile.getAbsolutePath());

        try {
            multipart.transferTo(testFile);
        } catch (IOException e) {
            logger.error("Error saving test file with name {}", multipart.getOriginalFilename(), e);
            throw new RuntimeException(e);
        }

    }

}
