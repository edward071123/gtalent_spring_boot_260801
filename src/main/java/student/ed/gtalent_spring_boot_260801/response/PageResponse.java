package student.ed.gtalent_spring_boot_260801.response;

import java.util.List;

public class PageResponse<T> {

    // 物件內容
    private List<T> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;


    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = countTotalPage(totalElements, size);
    }

    private int countTotalPage(long totalElements, int size) {
        // ex: 53個數量, 一頁10個
        // 算式為 53/10 => 5 ... 餘 3
        // 頁數就是 5 + 1 = 6 頁

        // ex: 50個數量, 一頁10個
        // 算式為 50/10 => 5 ... 餘 0
        // 頁數就是 5 頁

        int page = 0;
        page = (int) (totalElements/size);
        if (totalElements%size != 0) {
            // 有餘數 要多+1頁
            page = page + 1;
        } 

        return page;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
} 
