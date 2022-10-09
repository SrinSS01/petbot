package me.srin.petbot.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepo extends JpaRepository<User, User.ID> {
    @Query("select u from User u where u.userId = ?1 and u.guildId = ?2")
    User findByUserIdAndGuildId(long userId, long guildId);
}