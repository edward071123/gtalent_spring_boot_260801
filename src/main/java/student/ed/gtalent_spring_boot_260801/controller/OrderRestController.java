package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    @GetMapping("/{orderId}/status")
    public String getOrderStatus(@PathVariable Long orderId) {
        // 實際開發中，這裡會從資料庫查詢訂單狀態
        // 為了範例簡單，直接回傳字串
        String[] statuses = {"已下單", "處理中", "已出貨", "已完成"}; 
        return statuses[new java.util.Random().nextInt(statuses.length)];
    }
}
