package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 啟用一個簡單的記憶體訊息代理，客戶端訂閱這個前綴來接收訊息
        config.enableSimpleBroker("/topic");
        // 客戶端發送訊息給伺服器的前綴
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 註冊 WebSocket 端點，前端透過這個 endpoint 進行連線，並開啟 SockJS 備援
        registry.addEndpoint("/ws-order").setAllowedOriginPatterns("*").withSockJS();
    }
}
