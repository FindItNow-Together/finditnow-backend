package com.finditnow.userservice.grpc;

import com.finditnow.userservice.*;
import com.finditnow.userservice.dao.UserDao;
import com.finditnow.userservice.entity.User;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    private final UserDao userDao;

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

        var profile = UserProfile.newBuilder().setId(req.getId()).setEmail(req.getEmail()).setName(req.getName())
                .build();

        resp.onNext(UserProfileResponse.newBuilder().setUser(profile).build());
        resp.onCompleted();
    }

    @Override
    public void updateUserRole(UpdateUserRoleRequest request, StreamObserver<UserRoleUpdateResponse> responseObserver) {
        User user = userDao.findById(UUID.fromString(request.getId())).orElse(null);

        if (user == null) {
            responseObserver.onNext(UserRoleUpdateResponse.newBuilder().setId(request.getId())
                    .setMessage("Update failed").setError("No such user").build());
            responseObserver.onCompleted();
            return;
        }

        user.setRole(request.getRole());

        userDao.save(user);

        responseObserver.onNext(UserRoleUpdateResponse.newBuilder().setId(request.getId())
                .setMessage("Role updated successfully to: " + request.getRole()).build());
        responseObserver.onCompleted();
    }
}
