package com.finditnow.userservice.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.interservice.InterServiceClient;
import com.finditnow.user.*;
import com.finditnow.userservice.dao.UserDao;
import com.finditnow.userservice.dto.CreateDeliveryAgentRequest;
import com.finditnow.userservice.entity.User;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@Slf4j
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    private final UserDao userDao;
    private static final ObjectMapper objMapper = new ObjectMapper();

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void createUserProfile(CreateUserProfileRequest req, StreamObserver<UserProfileResponse> resp) {

        User user = new User();
        user.setId(UUID.fromString(req.getId()));
        user.setEmail(req.getEmail());
        user.setFirstName(req.getName());
        user.setRole(req.getRole());

        userDao.save(user);

        if ("DELIVERY_AGENT".equals(req.getRole())) {
            try {
                InterServiceClient.call("delivery-service", "/delivery-agent/add", "POST", objMapper.writeValueAsString(
                        CreateDeliveryAgentRequest.builder().agentId(user.getId()).build()
                ));
            }catch (Exception e) {
                log.error("failed to call delivery-service for delivery agent creation {}",
                        user.getId(), e);
                throw new RuntimeException("Internal server error", e);
            }
        }

        var profile = UserProfile.newBuilder().setId(req.getId()).setEmail(req.getEmail()).setName(req.getName()).build();

        resp.onNext(UserProfileResponse.newBuilder().setUser(profile).build());
        resp.onCompleted();
    }

    @Override
    public void updateUserRole(UpdateUserRoleRequest request, StreamObserver<UserRoleUpdateResponse> responseObserver) {
        User user = userDao.findById(UUID.fromString(request.getId())).orElse(null);

        if (user == null) {
            responseObserver.onNext(UserRoleUpdateResponse.newBuilder().setId(request.getId()).setMessage("Update failed").setError("No such user").build());
            responseObserver.onCompleted();
            return;
        }


        user.setRole(request.getRole());

        userDao.save(user);

        responseObserver.onNext(UserRoleUpdateResponse.newBuilder().setId(request.getId()).setMessage("Role updated successfully to: " + request.getRole()).build());
        responseObserver.onCompleted();
    }
}
