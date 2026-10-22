package student.ed.gtalent_spring_boot_260801.response;

public record ExcelImportError(
    int row,
    String field,
    String message
) {}