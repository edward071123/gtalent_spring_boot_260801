package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.TextMessageContent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Service 
public class LineWebhookService {

    private final LineReplyService lineReplyService;
    private final ChatClient chatClient;

    public LineWebhookService(LineReplyService lineReplyService, ChatClient.Builder builder) {
        this.lineReplyService = lineReplyService;
        this.chatClient = builder.build();
    }

    public void handleEvent(Event event) {
        if (event instanceof MessageEvent messageEvent) {
            handleMessageEvent(messageEvent);
        }
    }

    private void handleMessageEvent(MessageEvent event) {
        MessageContent message = event.message();

        if (message instanceof TextMessageContent textMessage) {
            String lineUserId = event.source().userId();
            String text = textMessage.text().trim();
            System.out.println("LINE webhook text message from " + lineUserId + ": " + text);

            String answer = "";
            try {
                answer = chatClient.prompt()
                    .user(text)
                    .call()
                    .content();
            } catch (RuntimeException exception) {
                // 暫時輸出完整例外，排查 Gemini API 呼叫失敗的原因。
                exception.printStackTrace();
                throw exception;
            }

            // String reply = switch (text.toLowerCase()) {
            //     case "ping" -> "pong";
            //     case "你好" -> "你好！我是群組機器人。";
            //     default -> "收到：" + text;
            // };

            lineReplyService.replyText(event.replyToken(), answer);
        }
    }
}
