package student.ed.gtalent_spring_boot_260801.service;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.exception.BookImportException;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BookExcelImportService {

    // 每次累積 2000 筆就寫入資料庫一次。
    // 這個數字不要太小，太小會讓資料庫往返次數太多；也不要太大，太大會讓記憶體壓力上升。
    private static final int BATCH_SIZE = 2000;

    // 錯誤明細最多回傳 100 筆，避免 Excel 有大量錯誤時 response 太大。
    private static final int MAX_ERROR_COUNT = 100;

    private final BookRepository bookRepository;

    public BookExcelImportService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // 匯入 Excel 的主要流程。
    //
    // 這裡特別採用「先驗證、後匯入」：
    // 1. 檔案本身的 request 檢查已經在 BookExcelImportRequest 完成。
    //    例如：有沒有上傳 file、file 是不是空檔、檔名是不是 .xlsx。
    // 2. Service 第一輪讀 Excel：只檢查 Excel 內容有沒有錯，不寫入資料庫。
    //    例如：標題列是不是 name / price、書名是否空白、價格是否為整數。
    // 3. 如果第一輪發現任何錯誤，直接丟出 BookImportException，資料庫完全不會新增資料。
    // 4. 第一輪全部通過後，第二輪才真的把資料批次寫入資料庫。
    //
    // 為什麼要讀兩次？
    // 如果邊讀邊寫，可能會發生：
    // - 第 1 到 50000 筆已經寫入成功
    // - 第 50001 筆才發現 price 格式錯誤
    // - 這時資料庫已經有一半資料，使用者很難修正
    //
    // 所以這裡寧願多讀一次 Excel，換取資料一致性：
    // - Excel 全部正確：才匯入
    // - Excel 任何一列錯誤：完全不匯入
    public int importBooks(MultipartFile file) {
        Map<String, String> errors = new TreeMap<>();

        // 第一輪只驗證，不寫入資料庫。
        // 好處是：如果第 99999 列錯了，前 99998 列也不會先被寫進資料庫。
        ImportResult validateResult = parseFirstSheet(file, false, errors);
        if (validateResult.getDataRowCount() == 0) {
            addError(errors, "file", ResponseMessages.getMessage(ResponseMessages.BOOK_EXCEL_NO_DATA));
        }
        throwIfHasErrors(errors);

        // 第二輪才真正批次寫入。
        // 這裡仍然用串流讀 Excel，不會一次把 10 萬筆全部放進記憶體。
        ImportResult importResult = parseFirstSheet(file, true, new TreeMap<>());
        return importResult.getImportedCount();
    }

    // 解析 Excel 第一個 sheet。
    //
    // writeToDatabase = false：
    // - 只驗證資料
    // - 不呼叫 bookRepository.batchCreate()
    //
    // writeToDatabase = true：
    // - 資料已經在第一輪驗證過
    // - 這一輪才開始分批寫入資料庫
    //
    // 這裡使用 Apache POI 的 SAX/Event API，而不是 XSSFWorkbook。
    //
    // XSSFWorkbook 的問題：
    // - 它會把整份 Excel workbook 載入記憶體
    // - 10 萬筆資料可能造成記憶體用量偏高
    //
    // SAX/Event API 的好處：
    // - 像讀流水一樣，一列一列處理
    // - 處理完一列就可以丟掉，不需要留住整份 Excel
    // - 比較適合 10 萬筆這種大量匯入
    private ImportResult parseFirstSheet(MultipartFile file, boolean writeToDatabase, Map<String, String> errors) {
        ImportResult result = new ImportResult();

        try (
                InputStream inputStream = file.getInputStream();
                OPCPackage opcPackage = OPCPackage.open(inputStream)
        ) {
            ReadOnlySharedStringsTable sharedStringsTable = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            StylesTable stylesTable = xssfReader.getStylesTable();
            Iterator<InputStream> sheetIterator = xssfReader.getSheetsData();

            if (!sheetIterator.hasNext()) {
                addError(errors, "file", ResponseMessages.getMessage(ResponseMessages.BOOK_EXCEL_FILE_INVALID));
                return result;
            }

            // 目前只讀第一個 sheet。新手練習時先把 Excel 格式固定下來，邏輯會比較清楚。
            try (InputStream sheetInputStream = sheetIterator.next()) {
                XMLReader parser = createXmlReader();
                BookSheetHandler sheetHandler = new BookSheetHandler(writeToDatabase, errors, result);

                // XSSFSheetXMLHandler 是 POI 提供的事件式讀取器。
                // 它會一格一格、逐列通知 BookSheetHandler，不會把整張 sheet 全部載入記憶體。
                ContentHandler contentHandler = new XSSFSheetXMLHandler(
                        stylesTable,
                        null,
                        sharedStringsTable,
                        sheetHandler,
                        new DataFormatter(),
                        false
                );

                parser.setContentHandler(contentHandler);
                parser.parse(new InputSource(sheetInputStream));
                sheetHandler.flushBatch();
            }
        } catch (BookImportException exception) {
            throw exception;
        } catch (Exception exception) {
            addError(errors, "file", ResponseMessages.getMessage(ResponseMessages.BOOK_EXCEL_FILE_INVALID));
        }

        return result;
    }

    private XMLReader createXmlReader() throws Exception {
        // SAXParserFactory 是 Java 標準 API，用來建立 SAX parser。
        //
        // 之前常見寫法是：
        // XMLReader parser = SAXHelper.newXMLReader();
        //
        // 但 POI 5.x 裡 SAXHelper.newXMLReader() 會出現 deprecated 警告。
        // deprecated 的意思是「目前還能用，但官方不建議新程式繼續依賴它」。
        //
        // 所以這裡改用 Java 標準的 SAXParserFactory：
        // - 不依賴 POI 的 deprecated helper
        // - 編譯時不會再出現該 deprecated 警告
        // - 仍然可以交給 XSSFSheetXMLHandler 做 Excel sheet 的串流解析
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        return saxParserFactory.newSAXParser().getXMLReader();
    }

    private void throwIfHasErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BookImportException(ResponseMessages.getMessage(ResponseMessages.BOOK_IMPORT_FAILED), errors);
        }
    }

    // 新增錯誤訊息到 errors map。
    //
    // key 範例：
    // - file：整份檔案的錯誤
    // - row1.header：第 1 列標題錯誤
    // - row5.name：第 5 列 name 欄位錯誤
    // - row8.price：第 8 列 price 欄位錯誤
    //
    // 為什麼最多只回傳 100 筆錯誤？
    // 如果 10 萬列全部都有錯，response 會非常巨大，前端和瀏覽器都不好處理。
    // 實務上通常先顯示前 100 筆，讓使用者修正檔案格式後再重新匯入。
    private void addError(Map<String, String> errors, String key, String message) {
        if (errors.size() < MAX_ERROR_COUNT) {
            errors.put(key, message);
            return;
        }

        errors.putIfAbsent("errors.limit", "錯誤超過 100 筆，只顯示前 100 筆，請先修正後再重新匯入");
    }

    private class BookSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final boolean writeToDatabase;

        private final Map<String, String> errors;

        private final ImportResult result;

        private final List<Book> batchBooks = new ArrayList<>(BATCH_SIZE);

        // 暫存目前這一列的欄位資料。key 是欄位位置，0 代表 A 欄，1 代表 B 欄。
        private final Map<Integer, String> currentRowValues = new TreeMap<>();

        private BookSheetHandler(boolean writeToDatabase, Map<String, String> errors, ImportResult result) {
            this.writeToDatabase = writeToDatabase;
            this.errors = errors;
            this.result = result;
        }

        @Override
        public void startRow(int rowNum) {
            // POI 準備開始讀新的一列。
            // 因為 currentRowValues 是暫存「目前這一列」的資料，
            // 所以每開始一列都要清空，避免上一列的資料殘留到下一列。
            currentRowValues.clear();
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null) {
                return;
            }

            int columnIndex = new CellReference(cellReference).getCol();

            // cellReference 範例：
            // - A1 代表第 1 列 A 欄
            // - B2 代表第 2 列 B 欄
            //
            // new CellReference(cellReference).getCol() 會把欄位轉成數字：
            // - A 欄 => 0
            // - B 欄 => 1
            //
            // 本匯入格式只需要：
            // - 第 0 欄 name
            // - 第 1 欄 price
            currentRowValues.put(columnIndex, trimToEmpty(formattedValue));
        }

        @Override
        public void endRow(int rowNum) {
            // POI 已經讀完這一列的所有 cell，現在可以開始檢查這列資料。
            //
            // rowNum 從 0 開始：
            // - rowNum = 0 是 Excel 第 1 列，這列是標題列 name / price
            // - rowNum = 1 是 Excel 第 2 列，這列開始才是書籍資料
            if (rowNum == 0) {
                validateHeaderRow();
                return;
            }

            handleDataRow(rowNum);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // 匯入書籍不需要頁首/頁尾，這裡留空即可。
        }

        private void validateHeaderRow() {
            String firstHeader = trimToEmpty(currentRowValues.get(0)).toLowerCase();
            String secondHeader = trimToEmpty(currentRowValues.get(1)).toLowerCase();

            // 為了讓匯入規則明確，第一列必須固定是：
            // A1 = name
            // B1 = price
            //
            // 這樣後端就不用猜「哪一欄是書名、哪一欄是價格」。
            // 對新手和使用者來說，也比較容易做出正確範本。
            if (!"name".equals(firstHeader) || !"price".equals(secondHeader)) {
                addError(errors, "row1.header", ResponseMessages.getMessage(ResponseMessages.BOOK_EXCEL_HEADER_INVALID));
            }
        }

        private void handleDataRow(int rowNum) {
            // 目前只讀 A 欄和 B 欄：
            // - A 欄：書名 name
            // - B 欄：價格 price
            //
            // 如果 Excel 還有 C、D、E 欄，本功能會先忽略。
            String name = trimToEmpty(currentRowValues.get(0));
            String priceText = trimToEmpty(currentRowValues.get(1));

            // Excel 第 2 列開始才是資料列；POI 的 rowNum 從 0 開始，所以要 +1 才是使用者看到的列號。
            int excelRowNumber = rowNum + 1;

            // 完全空白列直接跳過，方便使用者 Excel 下方有空列時不會被判定錯誤。
            if (name.isBlank() && priceText.isBlank()) {
                return;
            }

            result.incrementDataRowCount();

            boolean rowHasError = false;
            if (name.isBlank()) {
                addError(errors, "row" + excelRowNumber + ".name", ResponseMessages.getMessage(ResponseMessages.BOOK_NAME_REQUIRED));
                rowHasError = true;
            } else if (name.length() > 255) {
                addError(errors, "row" + excelRowNumber + ".name", "書名不可超過 255 個字");
                rowHasError = true;
            }

            Integer price = parsePrice(priceText, excelRowNumber);
            if (price == null) {
                rowHasError = true;
            }

            if (rowHasError || !writeToDatabase) {
                // 第一輪驗證時 writeToDatabase 是 false，所以即使資料正確也不寫入。
                // 第二輪匯入時 writeToDatabase 是 true，但如果這一列有錯也不寫入。
                return;
            }

            batchBooks.add(new Book(name, price));
            if (batchBooks.size() >= BATCH_SIZE) {
                flushBatch();
            }
        }

        private Integer parsePrice(String priceText, int excelRowNumber) {
            if (priceText.isBlank()) {
                addError(errors, "row" + excelRowNumber + ".price", ResponseMessages.getMessage(ResponseMessages.BOOK_PRICE_REQUIRED));
                return null;
            }

            try {
                // Excel 數字可能顯示成 1,000，所以先移除逗號再轉數字。
                BigDecimal priceDecimal = new BigDecimal(priceText.replace(",", ""));

                // intValueExact() 會要求 BigDecimal 必須剛好是整數。
                // 例如：
                // - 100  可以轉成 int
                // - 100.0 也可以轉成 int
                // - 100.5 會丟 ArithmeticException
                //
                // 這樣可以避免價格被匯入成小數。
                int price = priceDecimal.intValueExact();

                if (price < 1) {
                    addError(errors, "row" + excelRowNumber + ".price", ResponseMessages.getMessage(ResponseMessages.BOOK_PRICE_MIN));
                    return null;
                }

                return price;
            } catch (ArithmeticException | NumberFormatException exception) {
                addError(errors, "row" + excelRowNumber + ".price", "價格必須是整數");
                return null;
            }
        }

        private void flushBatch() {
            if (batchBooks.isEmpty()) {
                return;
            }

            // batchBooks 累積到 BATCH_SIZE，或 sheet 讀完時，就會呼叫這裡。
            //
            // 這裡一次送一批給 repository：
            // - 比 10 萬筆逐筆 insert 快很多
            // - 記憶體也只保留目前這一批，不保留全部 10 萬筆
            bookRepository.batchCreate(batchBooks);
            result.addImportedCount(batchBooks.size());
            batchBooks.clear();
        }
    }

    // 把 null 轉成空字串，並去掉前後空白。
    //
    // 這樣後面判斷欄位是否空白時，只要用 isBlank() 即可，
    // 不需要每次都額外判斷 value == null。
    private String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static class ImportResult {

        // Excel 內真正有內容的資料列數。
        // 空白列不會被算進來。
        private int dataRowCount;

        // 已成功寫入資料庫的筆數。
        // 第一輪驗證時不會增加，第二輪匯入時才會增加。
        private int importedCount;

        private void incrementDataRowCount() {
            this.dataRowCount++;
        }

        private int getDataRowCount() {
            return this.dataRowCount;
        }

        private void addImportedCount(int count) {
            this.importedCount += count;
        }

        private int getImportedCount() {
            return this.importedCount;
        }

    }

}
