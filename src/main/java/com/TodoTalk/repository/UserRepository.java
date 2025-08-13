package com.TodoTalk.repository;

import com.TodoTalk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    // Tìm kiếm user cho tính năng thêm vào nhóm
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
    
    // Tìm user không trong chat (để add vào nhóm)
    @Query("SELECT u FROM User u WHERE u.id NOT IN " +
           "(SELECT cp.user.id FROM ChatParticipant cp WHERE cp.chat.id = :chatId)")
    List<User> findUsersNotInChat(@Param("chatId") Long chatId);
}
