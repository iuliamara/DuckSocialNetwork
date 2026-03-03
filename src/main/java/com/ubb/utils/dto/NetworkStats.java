package com.ubb.utils.dto;

import com.ubb.domain.User;
import java.util.List;

/**
 * Record imuabil (DTO) utilizat pentru a transfera statisticile agregate ale retelei sociale.
 * Acum include si totalurile pentru utilizatori si prietenii.
 *
 * @param communityCount Numarul total de componente conexe (comunitati).
 * @param longestPathUsers Lista de utilizatori care formeaza drumul cel mai lung.
 * @param totalUsers Numarul total de utilizatori din sistem.
 * @param totalFriendships Numarul total de prietenii din sistem.
 */
public record NetworkStats(
        int communityCount,
        List<User> longestPathUsers,
        long totalUsers,        // Câmp nou
        long totalFriendships   // Câmp nou
) {
    // Record-urile genereaza automat constructorul si getterii.
}