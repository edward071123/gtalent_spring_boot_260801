package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        try {
            String answer = chatClient.prompt()
                    .user(request.message())
                    .call()
                    .content();
            return new ChatResponse(answer);
        } catch (RuntimeException exception) {
            // 暫時輸出完整例外，排查 Gemini API 呼叫失敗的原因。
            exception.printStackTrace();
            throw exception;
        }
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) {
    }

    public record ChatResponse(String answer) {
    }
}
