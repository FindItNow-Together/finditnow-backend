package com.finditnow.userservice.config;

import com.finditnow.config.Config;
import com.finditnow.user.UserServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(UserServiceGrpc.UserServiceImplBase userServiceImpl) throws Exception {
        System.out.println(">>> [GrpcServerConfig] Bean creation triggered");
        Server server = ServerBuilder
                .forPort(Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8083")))
                .addService(userServiceImpl)
                .build()
                .start();
        System.out.println(
                ">>> [GrpcServerConfig] gRPC Server Started on " + Config.get("USER_SERVICE_GRPC_PORT", "8083"));
        return server;
    }
}
