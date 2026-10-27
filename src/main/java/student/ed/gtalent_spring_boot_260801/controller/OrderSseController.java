package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sse")
public class OrderSseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/orders/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable Long orderId) {
        // 設定較長的逾時時間，例如 1 小時 (3600000 ms)，避免太快自動斷線
        SseEmitter emitter = new SseEmitter(3600_000L);

        emitters.put(orderId, emitter);
        System.out.println("【SSE】客戶端已成功連線，Order ID: " + orderId);

        // 監控連線結束事件
        emitter.onCompletion(() -> {
            System.out.println("【SSE】連線完成 (Completion)，Order ID: " + orderId);
            emitters.remove(orderId);
        });

        emitter.onTimeout(() -> {
            System.out.println("【SSE】連線逾時 (Timeout)，Order ID: " + orderId);
            emitter.complete();
            emitters.remove(orderId);
        });

        emitter.onError((ex) -> {
            System.out.println("【SSE】連線發生錯誤，Order ID: " + orderId + ", 錯誤訊息: " + ex.getMessage());
            emitters.remove(orderId);
        });

        // 連線建立後，可以先發送一個「Ping」或歡迎訊息確認通道暢通
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected success"));
        } catch (IOException e) {
            emitters.remove(orderId);
        }

        return emitter;
    }

    /* 更新觸發推送的測試 API
    curl -X POST "http://localhost:8080/api/sse/orders/123/push" \
     -H "Content-Type: application/json" \
     -d '{"status":"已完成"}'
    */
    // 模擬觸發推送的測試 API
    @PostMapping("/orders/{orderId}/push")
    public String triggerPush(@PathVariable Long orderId, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        SseEmitter emitter = emitters.get(orderId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("status-update").data(status));
                return "推送成功";
            } catch (IOException e) {
                emitters.remove(orderId);
                return "推送失敗";
            }
        }
        return "找不到該訂單的連線";
    }
}
