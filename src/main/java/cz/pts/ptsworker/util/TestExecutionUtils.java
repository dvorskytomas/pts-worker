package cz.pts.ptsworker.util;

import cz.pts.ptsworker.dto.TestExecutionDto;

public class TestExecutionUtils {

    public static String composeCommand(TestExecutionDto dto, String finalLogFileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("cd ");
        sb.append(dto.getToolDirectoryPath());
        sb.append(" && ");
        sb.append(dto.getToolRunCommand());
        String cmd = sb.toString();
        return cmd.replace(dto.getLogFileName(), finalLogFileName);
    }

    public static String composeFinalLogFileName(TestExecutionDto dto) {
        return dto.getWorkerNumber() + "_" + dto.getTestExecutionId() + "_" + dto.getLogFileName();
    }

}
