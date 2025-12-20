package com.finditnow.filegateway;

import com.finditnow.config.Config;
import com.finditnow.filegateway.auth.RefreshTokenValidator;
import com.finditnow.filegateway.handlers.DownloadHandler;
import com.finditnow.filegateway.handlers.UploadHandler;
import com.finditnow.filegateway.storage.FileStorage;
import com.finditnow.filegateway.storage.LocalFileStorage;
import com.finditnow.redis.RedisStore;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

import java.nio.file.Path;

public class FileGatewayApp {
    public static void main(String[] args) {
        RedisStore redisStore = RedisStore.getInstance();

        FileStorage storage =
                new LocalFileStorage(Path.of("/data/uploads"));

        RefreshTokenValidator sessionValidator =
                new RefreshTokenValidator(redisStore);

        BlockingHandler downloadHandler = new BlockingHandler(new DownloadHandler(storage, sessionValidator));
        BlockingHandler uploadHandler = new BlockingHandler(new UploadHandler(storage));

        RoutingHandler routes = new RoutingHandler()
                .post("/api/files/upload",
                        uploadHandler)
                .get("/api/files/{domain}/{entityId}/{purpose}/{file}",
                        downloadHandler);

        Undertow server = Undertow.builder()
                .addHttpListener(Integer.parseInt(Config.get("FILE_GATEWAY_SERVICE_PORT", "8090")), "localhost")
                .setHandler(routes)
                .build();

        server.start();
    }
}
