package com.finditnow.deliveryservice.config;

import com.finditnow.deliveryservice.controller.DeliveryController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TicketHandshakeInterceptor implements HandshakeInterceptor {
    private final DeliveryController ticketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Extract ticket from query param: ?ticket=...
        String query = request.getURI().getQuery();
        String ticket = query.split("ticket=")[1];

        String username = ticketService.getUsernameFromTicket(ticket);

        if (username != null) {
            attributes.put("user", username); // Save for the session
            return true;
        }
        return false; // Reject connection
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}