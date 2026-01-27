package com.finditnow.auth.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.LoggerFactory;

public class RequestLoggingHandler implements HttpHandler {
    private final HttpHandler next;

    public RequestLoggingHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // this moves the execution to worker thread for blocking calls
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();

        long start = System.nanoTime();

        exchange.addExchangeCompleteListener((ex, nextListener) -> {
            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            LoggerFactory.getLogger(RequestLoggingHandler.class)
                    .info(
                            "{} {} -> {} ({} ms)",
                            ex.getRequestMethod(),
                            ex.getRequestPath(),
                            ex.getStatusCode(),
                            durationMs
                    );

            nextListener.proceed();
        });

        next.handleRequest(exchange);
    }
}
