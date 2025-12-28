package com.finditnow.auth.config;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GrpcHealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(GrpcHealthChecker.class);
    private GrpcHealthChecker() {}

    public static void waitForGrpcServer(
            String host,
            int port,
            int maxRetries,
            long delayMillis
    ) throws InterruptedException {

        logger.info("Checking gRPC server availability at {}:{}", host, port);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            ManagedChannel channel = null;
            try {
                channel = ManagedChannelBuilder
                        .forAddress(host, port)
                        .usePlaintext()
                        .build();

                ConnectivityState state = channel.getState(true); // force connect

                if (state == ConnectivityState.READY) {
                    logger.info("gRPC server is READY on attempt {}", attempt);
                    return;
                }

                // Wait briefly for state transition
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

                while (System.nanoTime() < deadline) {
                    state = channel.getState(false);
                    if (state == ConnectivityState.READY) {
                        logger.info("gRPC server became READY on attempt {}", attempt);
                        return;
                    }
                    Thread.sleep(100);
                }

                logger.warn("Attempt {} failed, state={}", attempt, state);

            } catch (Exception e) {
                logger.warn("Attempt {} failed with exception", attempt, e);
            } finally {
                if (channel != null) {
                    channel.shutdownNow();
                }
            }

            if (attempt < maxRetries) {
                Thread.sleep(delayMillis);
            }
        }

        throw new IllegalStateException(
                "gRPC server NOT reachable after " + maxRetries + " attempts"
        );
    }
}
