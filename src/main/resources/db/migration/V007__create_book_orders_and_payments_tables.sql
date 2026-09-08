CREATE TABLE book_orders (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '訂單流水號',
    order_no VARCHAR(64) NOT NULL COMMENT '系統訂單編號；會對應藍新的 MerchantOrderNo',
    book_id BIGINT NOT NULL COMMENT '購買的書籍 ID',
    buyer_member_id BIGINT NOT NULL COMMENT '買家會員 ID',
    amount INT NOT NULL COMMENT '訂單金額；建立訂單時固定，避免付款時被書價異動影響',
    order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '訂單狀態：PENDING_PAYMENT 待付款、PAID 已付款、CANCELLED 已取消、FAILED 付款失敗',
    created_at DATETIME NOT NULL COMMENT '建立時間',
    updated_at DATETIME NOT NULL COMMENT '最後更新時間',
    paid_at DATETIME NULL COMMENT '付款成功時間',
    cancelled_at DATETIME NULL COMMENT '訂單取消時間',
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_orders_order_no (order_no),
    KEY idx_book_orders_book_id (book_id),
    KEY idx_book_orders_buyer_member_id (buyer_member_id),
    KEY idx_book_orders_order_status (order_status)
) COMMENT='書籍交易訂單；記錄會員向平台購買某本書的一次交易意圖';

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '付款紀錄流水號',
    order_id BIGINT NOT NULL COMMENT '對應 book_orders.id',
    merchant_order_no VARCHAR(64) NOT NULL COMMENT '送給藍新的商店訂單編號 MerchantOrderNo',
    merchant_id VARCHAR(32) NOT NULL COMMENT '藍新商店代號 MerchantID',
    provider VARCHAR(32) NOT NULL DEFAULT 'NEWEBPAY' COMMENT '金流服務商；目前固定為 NEWEBPAY',
    provider_trade_no VARCHAR(64) NULL COMMENT '藍新交易序號 TradeNo；藍新回傳後寫入',
    payment_method VARCHAR(32) NULL COMMENT '付款方式，例如 CREDIT、WEBATM、VACC、CVS',
    amount INT NOT NULL COMMENT '付款金額；必須與訂單金額一致',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '付款狀態：INIT 已建立、PENDING 待付款、PAID 已付款、FAILED 失敗、CANCELLED 已取消、REFUNDED 已退款',
    trade_sha VARCHAR(128) NULL COMMENT '建立付款時產生的 TradeSha；用於除錯與對帳',
    return_code VARCHAR(32) NULL COMMENT '藍新回傳狀態碼',
    return_message VARCHAR(255) NULL COMMENT '藍新回傳訊息',
    paid_at DATETIME NULL COMMENT '藍新確認付款成功時間',
    created_at DATETIME NOT NULL COMMENT '建立時間',
    updated_at DATETIME NOT NULL COMMENT '最後更新時間',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_merchant_order_no (merchant_order_no),
    KEY idx_payments_order_id (order_id),
    KEY idx_payments_provider_trade_no (provider_trade_no),
    KEY idx_payments_payment_status (payment_status)
) COMMENT='金流付款紀錄；一筆訂單對應一次藍新付款流程';


CREATE TABLE payment_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '金流通知流水號',
    payment_id BIGINT NULL COMMENT '對應 payments.id；解析前或找不到付款時可為 NULL',
    merchant_order_no VARCHAR(64) NULL COMMENT '藍新通知中的 MerchantOrderNo',
    provider VARCHAR(32) NOT NULL DEFAULT 'NEWEBPAY' COMMENT '金流服務商；目前固定為 NEWEBPAY',
    provider_trade_no VARCHAR(64) NULL COMMENT '藍新交易序號 TradeNo',
    notify_status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED' COMMENT '通知處理狀態：RECEIVED 已收到、PROCESSED 已處理、FAILED 處理失敗、IGNORED 重複或無需處理',
    raw_payload TEXT NOT NULL COMMENT '藍新 NotifyURL 原始回傳內容；保留用於查帳與問題排查',
    verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否通過 TradeSha 或資料驗證；1 是、0 否',
    received_at DATETIME NOT NULL COMMENT '收到通知時間',
    processed_at DATETIME NULL COMMENT '處理完成時間',
    error_message VARCHAR(500) NULL COMMENT '處理失敗原因',
    PRIMARY KEY (id),
    KEY idx_payment_notifications_payment_id (payment_id),
    KEY idx_payment_notifications_merchant_order_no (merchant_order_no),
    KEY idx_payment_notifications_provider_trade_no (provider_trade_no),
    KEY idx_payment_notifications_notify_status (notify_status),
    KEY idx_payment_notifications_received_at (received_at)
) COMMENT='金流通知紀錄；保存藍新 NotifyURL 回呼，支援重送、查帳與除錯';
