package me.srin.petbot.database;

import lombok.*;
import me.srin.petbot.utils.Config;
import net.dv8tion.jda.api.entities.Member;
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
    public static final Map<Member, PetLab> MEMBER_PET_LAB_MAP = new HashMap<>();
    public static final Map<Long, Long> MESSAGE_PET_STATUS_MAP_SELECTED = new HashMap<>();

    @Getter @Setter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PetLab {
        private ScheduledFuture<?> task;
        private Pet pet;
    }
}
