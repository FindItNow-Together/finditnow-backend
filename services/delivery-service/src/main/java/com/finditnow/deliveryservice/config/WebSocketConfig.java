package com.finditnow.deliveryservice.config;

import com.finditnow.deliveryservice.controller.DeliveryLocationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final TicketHandshakeInterceptor ticketInterceptor;
    private final DeliveryLocationHandler deliveryLocationHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deliveryLocationHandler, "/ws-location")
                .addInterceptors(ticketInterceptor)
                .setAllowedOrigins("*");
    }
}
