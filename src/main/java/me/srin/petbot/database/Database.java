package me.srin.petbot.database;

import lombok.*;
import me.srin.petbot.utils.Config;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
@AllArgsConstructor
@Getter @Setter
public class Database {
    private final PetRepo petRepo;
    private final PetUserRDBMSRepository petUserRDBMSRepository;
    private final Config config;
    public static final Map<Long /*guild id*/, Map<Long/*user id*/, PetLab>> MEMBER_PET_LAB_MAP = new HashMap<>();
    public static final Map<Long, Long> MESSAGE_PET_STATUS_MAP_SELECTED = new HashMap<>();
    public static final Map<Long, Pet> PET_MAP = new HashMap<>();

    @Getter @Setter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PetLab {
        private ScheduledFuture<?> task;
        private Pet pet;
    }
}
