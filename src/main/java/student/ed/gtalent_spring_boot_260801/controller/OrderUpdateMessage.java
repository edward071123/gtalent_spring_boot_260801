package student.ed.gtalent_spring_boot_260801.controller;

public class OrderUpdateMessage {
    private Long orderId;   // 訂單編號
    private String status;  // 訂單狀態（例如：待處理、製作中...）

    // 無參數建構子（Spring 進行 JSON 反序列化時必備）
    public OrderUpdateMessage() {}

    // 有參數建構子（方便程式碼內部建立物件使用）
    public OrderUpdateMessage(Long orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    // Getters 與 Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
