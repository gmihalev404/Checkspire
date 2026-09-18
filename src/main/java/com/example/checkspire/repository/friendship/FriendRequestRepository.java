package com.example.checkspire.repository.friendship;

import com.example.checkspire.model.entity.friendship.FriendRequest;
import com.example.checkspire.model.entity.user.User;
import com.example.checkspire.model.enums.friendship.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRequestRepository
        extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverAndStatus(
            User receiver,
            FriendRequestStatus status
    );

    List<FriendRequest> findBySenderAndStatus(
            User sender,
            FriendRequestStatus status
    );

    boolean existsBySenderAndReceiverAndStatus(
            User sender,
            User receiver,
            FriendRequestStatus status
    );
}
