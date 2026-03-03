package com.ubb.repository;

import com.ubb.domain.Friendship;
import com.ubb.domain.Tuple;
import com.ubb.utils.dto.FriendshipDTO;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.util.List;

public interface FriendshipRepositoryInterface extends PagingRepository<Tuple<Long, Long>, Friendship> {
    //metode mosteniste
    List<Long> findFriendsOf(Long userId);

    /**
     * Gaseste toate inregistrarile Friendship care il includ pe userId.
     * @param userId ID-ul utilizatorului.
     * @return Lista de obiecte Friendship complete.
     */
    List<Friendship> findFriendshipsOf(Long userId);

    Page<FriendshipDTO> findAllPaginatedForUser(Pageable pageable, Long userId);
}
