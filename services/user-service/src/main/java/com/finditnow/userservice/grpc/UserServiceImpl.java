package com.finditnow.userservice.grpc;

import com.finditnow.user.CreateUserProfileRequest;
import com.finditnow.user.UserProfile;
import com.finditnow.user.UserProfileResponse;
import com.finditnow.user.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void createUserProfile(CreateUserProfileRequest req,
                                  StreamObserver<UserProfileResponse> resp) {

        var profile = UserProfile.newBuilder()
                .setId(req.getId())
                .setEmail(req.getEmail())
                .setName(req.getName())
                .build();

        resp.onNext(UserProfileResponse.newBuilder().setUser(profile).build());
        resp.onCompleted();
    }
}
