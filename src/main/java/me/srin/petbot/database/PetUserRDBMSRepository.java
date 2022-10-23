package me.srin.petbot.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PetUserRDBMSRepository extends JpaRepository<PetUserRDBMS, PetUserRDBMS.ID> {
    @Query("select p.petId from PetUserRDBMS p where p.memberId = ?1")
    List<Long> findByMemberId(long memberId);

    @Query("select p from PetUserRDBMS p where p.petId = ?1")
    Optional<PetUserRDBMS> findByPetId(long petId);

}