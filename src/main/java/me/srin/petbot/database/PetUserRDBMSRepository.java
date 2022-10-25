package me.srin.petbot.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PetUserRDBMSRepository extends JpaRepository<PetUserRDBMS, PetUserRDBMS.ID> {
    @Query("select p.petId from PetUserRDBMS p where p.memberId = ?1 and p.guildId = ?2")
    List<Long> findByMemberIdAndGuildId(long memberId, long guildId);

    @Query("select p from PetUserRDBMS p where p.guildId = ?1 and p.petId = ?2")
    Optional<PetUserRDBMS> findByGuildIdAndPetId(long guildId, long petId);

}