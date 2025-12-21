package com.finditnow.filegateway;

import com.finditnow.config.Config;
import com.finditnow.filegateway.auth.RefreshTokenValidator;
import com.finditnow.filegateway.handlers.CorsHandler;
import com.finditnow.filegateway.handlers.DownloadHandler;
import com.finditnow.filegateway.handlers.UploadHandler;
import com.finditnow.filegateway.storage.FileStorage;
import com.finditnow.filegateway.storage.LocalFileStorage;
import com.finditnow.redis.RedisStore;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;

import java.nio.file.Path;

public class FileGatewayApp {
    public static void main(String[] args) {
        RedisStore redisStore = RedisStore.getInstance();

        MultiPartParserDefinition multiPartParserDefinition = new MultiPartParserDefinition()
                .setTempFileLocation(Path.of(System.getProperty("java.io.tmpdir")));

        multiPartParserDefinition.setMaxIndividualFileSize(1024 * 1024 * 10L);


        FileStorage storage =
                new LocalFileStorage(Path.of("data", "uploads"));

        RefreshTokenValidator sessionValidator =
                new RefreshTokenValidator(redisStore);

        BlockingHandler downloadHandler = new BlockingHandler(new DownloadHandler(storage, sessionValidator));
        BlockingHandler uploadHandler = new BlockingHandler(new UploadHandler(storage, multiPartParserDefinition));

        RoutingHandler routes = new RoutingHandler()
                .post("/upload",
                        uploadHandler)
                .get("/download/{domain}/{entityId}/{purpose}/{file}",
                        downloadHandler);

        FormParserFactory formParserFactory = new FormParserFactory.Builder().addParser(multiPartParserDefinition).build();

        HttpHandler formHandler =
                new EagerFormParsingHandler(formParserFactory).setNext(routes);

        CorsHandler handler = new CorsHandler(formHandler);

        Undertow server = Undertow.builder()
                .addHttpListener(Integer.parseInt(Config.get("FILE_GATEWAY_SERVICE_PORT", "8090")), "localhost")
                .setHandler(handler)
                .build();

        server.start();
    }
}
