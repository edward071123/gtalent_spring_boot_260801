package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.stereotype.Service;

import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.TextMessageContent;

@Service 
public class LineWebhookService {

    private final LineReplyService lineReplyService;

    public LineWebhookService(LineReplyService lineReplyService) {
        this.lineReplyService = lineReplyService;
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

            String reply = switch (text.toLowerCase()) {
                case "ping" -> "pong";
                case "你好" -> "你好！我是群組機器人。";
                default -> "收到：" + text;
            };

            lineReplyService.replyText(event.replyToken(), reply);
        }
    }
}
