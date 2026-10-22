package student.ed.gtalent_spring_boot_260801.controller;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import student.ed.gtalent_spring_boot_260801.response.ExcelImportResponse;
import student.ed.gtalent_spring_boot_260801.service.ExcelService;

@RestController
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelService excelService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export()
            throws IOException {

        byte[] excel = excelService.exportMembers();

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=members.xlsx"
            )
            .contentType(
                MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
            .body(excel);
    }


   @PostMapping(
    value = "/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ExcelImportResponse> importExcel(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        System.out.println("收到檔案：" + file.getOriginalFilename());

        ExcelImportResponse result =
            excelService.importMembers(file);

        return result.success()
            ? ResponseEntity.ok(result)
            : ResponseEntity.badRequest().body(result);
    }
}
