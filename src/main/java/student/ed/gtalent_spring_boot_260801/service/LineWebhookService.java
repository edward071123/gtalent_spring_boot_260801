package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.stereotype.Service;

import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageContent;
import com.linecorp.bot.webhook.model.TextMessageContent;

@Service 
public class LineWebhookService {
    
     public void handleEvent(Event event) {
        if (event instanceof MessageEvent messageEvent) {
            handleMessageEvent(messageEvent);
        }
    }

    private void handleMessageEvent(MessageEvent event) {
        MessageContent message = event.message();

        if (message instanceof TextMessageContent textMessage) {
            String lineUserId = event.source().userId();
            String text = textMessage.text();
            System.out.println("LINE webhook text message from " + lineUserId + ": " + text);
        }
    }
}