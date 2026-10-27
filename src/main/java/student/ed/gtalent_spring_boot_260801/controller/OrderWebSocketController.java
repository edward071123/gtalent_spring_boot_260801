package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class OrderWebSocketController {

    /**
     * 當前端透過 WebSocket 發送訊息到 "/app/update-status" 時，
     * Spring Boot 會自動進到這個方法處理。
     */
    @MessageMapping("/update-status") // 接收前端送來的目的地
    @SendTo("/topic/orders")       // 將回傳結果廣播給所有訂閱 "/topic/orders" 的客戶端
    public String handleOrderStatusUpdate(OrderUpdateMessage message) {
        // 1. 處理你的業務邏輯（例如更新資料庫）
        System.out.println("收到前端指令，訂單 ID: " + message.getOrderId() + " 狀態改為: " + message.getStatus());

        // 2. 回傳最新的狀態（會自動廣播給訂閱者）
        return "訂單 " + message.getOrderId() + " 已更新為: " + message.getStatus();
    }
}
