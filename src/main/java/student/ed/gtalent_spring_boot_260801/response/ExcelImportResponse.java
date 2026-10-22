package student.ed.gtalent_spring_boot_260801.response;

import java.util.List;

public record ExcelImportResponse(
    boolean success,
    int total,
    int inserted,
    int updated,
    int failed,
    String message,
    List<ExcelImportError> errors
) {}