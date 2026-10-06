package student.ed.gtalent_spring_boot_260801.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linecorp.bot.spring.boot.web.argument.annotation.LineBotMessages;
import com.linecorp.bot.webhook.model.Event;

import student.ed.gtalent_spring_boot_260801.service.LineWebhookService;

@RestController
public class LineWebhookController {
    private final LineWebhookService lineWebhookService;

    public LineWebhookController(LineWebhookService lineWebhookService) {
        this.lineWebhookService = lineWebhookService;
    }

    @PostMapping("${line.bot.handler.path:/api/line/webhook}")
    public ResponseEntity<Void> webhook(@LineBotMessages List<Event> events) {
        for (Event event : events) {
            lineWebhookService.handleEvent(event);
        }

        return ResponseEntity.ok().build();
    }
}
