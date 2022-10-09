package me.srin.petbot.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PetUserRDBMSRepository extends JpaRepository<PetUserRDBMS, PetUserRDBMS.ID> {
    @Query("select p.petId from PetUserRDBMS p where p.userId = ?1 and p.guildId = ?2")
    List<Long> findByUserIdAndGuildId(long userId, long guildId);

    @Query("select count(p) from PetUserRDBMS p where p.userId = ?1 and p.guildId = ?2")
    long countByUserIdAndGuildId(long userId, long guildId);

}