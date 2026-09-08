package student.ed.gtalent_spring_boot_260801.constant;

public final class NotifyStatus {

    // 已收到藍新 NotifyURL 通知，但尚未完成驗證與狀態更新。
    public static final String RECEIVED = "RECEIVED";

    // 已完成驗證、解密與訂單/付款狀態更新。
    public static final String PROCESSED = "PROCESSED";

    // 通知格式不正確、驗證失敗、找不到付款資料或處理過程發生錯誤。
    public static final String FAILED = "FAILED";

    // 重複通知或業務上不需要處理的通知。
    public static final String IGNORED = "IGNORED";

    private NotifyStatus() {
    }
}
