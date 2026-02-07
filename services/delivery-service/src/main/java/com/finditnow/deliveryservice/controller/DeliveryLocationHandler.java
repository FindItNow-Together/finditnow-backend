package com.finditnow.deliveryservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeliveryLocationHandler extends TextWebSocketHandler {

    // Map of OrderId -> List of Session IDs (to handle multiple listeners like Customer + Support)
    private final Map<String, Set<WebSocketSession>> orderSubscriptions = new ConcurrentHashMap<>();

    // Map to track which Session belongs to which Order (for easy cleanup on disconnect)
    private final Map<String, String> sessionToOrder = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        String type = json.get("type").asText(); // "JOIN" or "LOCATION_UPDATE"
        String orderId = json.get("orderId").asText();

        if ("JOIN".equals(type)) {
            // Register this session to an order (Customer or Driver)
            orderSubscriptions.computeIfAbsent(orderId, k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionToOrder.put(session.getId(), orderId);
            System.out.println("Session " + session.getId() + " joined order: " + orderId);
        }
        else if ("LOCATION_UPDATE".equals(type)) {
            // This is the Driver sending their coordinates
            broadcastToOrder(orderId, message, session.getId());
        }
    }

    private void broadcastToOrder(String orderId, TextMessage message, String senderId) {
        Set<WebSocketSession> subscribers = orderSubscriptions.get(orderId);
        if (subscribers != null) {
            subscribers.forEach(s -> {
                try {
                    // Don't send the update back to the sender (the Driver)
                    if (s.isOpen() && !s.getId().equals(senderId)) {
                        s.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String orderId = sessionToOrder.remove(session.getId());
        if (orderId != null && orderSubscriptions.containsKey(orderId)) {
            orderSubscriptions.get(orderId).remove(session);
        }
        System.out.println("Connection closed: " + session.getId());
    }
}
