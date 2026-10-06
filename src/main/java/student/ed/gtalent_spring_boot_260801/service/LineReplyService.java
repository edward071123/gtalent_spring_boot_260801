package student.ed.gtalent_spring_boot_260801.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;

import student.ed.gtalent_spring_boot_260801.exception.ApiException;

@Service
public class LineReplyService {

    private final MessagingApiClient messagingApiClient;

    public LineReplyService(MessagingApiClient messagingApiClient) {
        this.messagingApiClient = messagingApiClient;
    }

    public void replyText(String replyToken, String message) {
        ReplyMessageRequest request = new ReplyMessageRequest(
                replyToken,
                List.of(new TextMessage(message)),
                false);

        try {
            messagingApiClient.replyMessage(request).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException("line", "LINE 回覆被中斷");
        } catch (ExecutionException exception) {
            throw new ApiException("line", "LINE 回覆失敗");
        }
    }
}