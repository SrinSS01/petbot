package me.srin.petbot.database;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepo extends JpaRepository<Pet, Long> {
    @Override
    boolean existsById(@NotNull Long id);
}
