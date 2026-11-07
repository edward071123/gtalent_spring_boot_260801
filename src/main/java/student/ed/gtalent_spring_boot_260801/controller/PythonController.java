package student.ed.gtalent_spring_boot_260801.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/python")
public class PythonController {

    private final String pythonExecutable;
    private final String scriptPath;
    private final String faceDetectScriptPath;

    public PythonController(
            @Value("${python.executable:python3}") String pythonExecutable,
            @Value("${python.script-path:/Users/edwardchung/taskers/da_shu_zhi_gtalent/spring_python/calculate.py}") String scriptPath,
            @Value("${python.face-detect-script-path:/Users/edwardchung/taskers/da_shu_zhi_gtalent/spring_python/face_detect.py}") String faceDetectScriptPath) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.faceDetectScriptPath = faceDetectScriptPath;
    }

    @GetMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public String calculate() throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                scriptPath,
                "10",
                "20");

        pb.redirectErrorStream(true);

        Process process = pb.start();

        String result;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            result = reader.lines()
                    .collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Python 執行失敗：" + result);
        }

        return result;
    }
    
    @PostMapping("/face-detect")
    public ResponseEntity<?> detect(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body("請上傳圖片");
        }

        Path tempFile = Files.createTempFile(
            "opencv-", ".jpg"
        );

        try {
            file.transferTo(tempFile);

            ProcessBuilder pb = new ProcessBuilder(
                pythonExecutable,
                faceDetectScriptPath,
                tempFile.toString()
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            boolean finished;
            String result;

            // 使用背景執行緒讀取輸出，避免緩衝區阻塞
            ExecutorService executor =
                Executors.newSingleThreadExecutor();

            try {
                Future<String> output = executor.submit(() -> {
                    try (BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                process.getInputStream(),
                                StandardCharsets.UTF_8))) {

                        return reader.lines()
                            .collect(Collectors.joining("\n"));
                    }
                });

                finished = process.waitFor(
                    30, TimeUnit.SECONDS
                );

                if (!finished) {
                    process.destroyForcibly();
                    return ResponseEntity.status(504)
                        .body("Python 執行逾時");
                }

                result = output.get(5, TimeUnit.SECONDS);

            } finally {
                executor.shutdownNow();
            }

            if (process.exitValue() != 0) {
                return ResponseEntity.internalServerError()
                    .body(result);
            }

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
