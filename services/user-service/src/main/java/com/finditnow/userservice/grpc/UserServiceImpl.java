package com.finditnow.userservice.grpc;

import com.finditnow.user.CreateUserProfileRequest;
import com.finditnow.user.UserProfile;
import com.finditnow.user.UserProfileResponse;
import com.finditnow.user.UserServiceGrpc;
import com.finditnow.userservice.dto.UserDto;
import com.finditnow.userservice.service.UserService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    private final UserService userService;

    public UserServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void createUserProfile(CreateUserProfileRequest req,
                                  StreamObserver<UserProfileResponse> resp) {

        UserDto user = new UserDto();
        user.setId(UUID.fromString(req.getId()));
        user.setEmail(req.getEmail());
        user.setFirstName(req.getName());
        user.setRole(req.getRole());

        userService.createUser(user);

        var profile = UserProfile.newBuilder()
                .setId(req.getId())
                .setEmail(req.getEmail())
                .setName(req.getName())
                .build();

        resp.onNext(UserProfileResponse.newBuilder().setUser(profile).build());
        resp.onCompleted();
    }
}
