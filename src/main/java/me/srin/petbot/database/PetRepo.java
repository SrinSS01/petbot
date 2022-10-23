package me.srin.petbot.database;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PetRepo extends JpaRepository<Pet, Long> {
    @Override
    boolean existsById(@NotNull Long id);

    @Query(value = "select r.`rank` from (select p.id, rank() over (order by p.total_xp desc) `rank` from pets p) r where r.id=?1", nativeQuery = true)
    int getRankById(long id);

}
