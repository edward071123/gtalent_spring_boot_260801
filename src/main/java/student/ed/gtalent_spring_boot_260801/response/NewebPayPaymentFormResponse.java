package student.ed.gtalent_spring_boot_260801.response;

import lombok.Getter;

@Getter
public class NewebPayPaymentFormResponse {

    private String gatewayUrl;
    private String merchantId;
    private String version;
    private String tradeInfo;
    private String tradeSha;

    public NewebPayPaymentFormResponse(
            String gatewayUrl,
            String merchantId,
            String version,
            String tradeInfo,
            String tradeSha) {
        this.gatewayUrl = gatewayUrl;
        this.merchantId = merchantId;
        this.version = version;
        this.tradeInfo = tradeInfo;
        this.tradeSha = tradeSha;
    }
}
