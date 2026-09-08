package student.ed.gtalent_spring_boot_260801.constant;

public final class OrderStatus {

    // 訂單已建立，但尚未完成付款。
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";

    // 訂單已完成付款，可進入後續出貨或完成交易流程。
    public static final String PAID = "PAID";

    // 訂單已取消，例如使用者放棄付款或系統逾時取消。
    public static final String CANCELLED = "CANCELLED";

    // 訂單付款失敗，通常由金流回傳失敗結果後更新。
    public static final String FAILED = "FAILED";

    private OrderStatus() {
    }
}
