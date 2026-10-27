package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    /*
    curl -X POST "http://localhost:8080/api/admin/orders/123/status" \
     -H "Content-Type: application/json" \
     -d '{"status":"已完成"}'
    */
    // 直接在 Controller 注入 WebSocket 訊息模板
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            
            // TODO: 如果有需要，可以在這裡直接寫入資料庫
            // orderRepository.updateStatus(orderId, status);

            // 直接透過 WebSocket 推播給所有訂閱該訂單的前端
            // 對應前端訂閱的路徑: /topic/order/{orderId}
            messagingTemplate.convertAndSend("/topic/order/" + orderId, status);
            
            System.out.println("訂單 " + orderId + " 狀態已更新為：" + status);
            return "訂單狀態更新成功並已即時推播！";
        } catch (Exception e) {
            return "更新失敗：" + e.getMessage();
        }
    }
}
