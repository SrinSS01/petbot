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
    private final UserRepo userRepo;
    private final PetRepo petRepo;
    private final PetUserRDBMSRepository petUserRDBMSRepository;
    private final Config config;
    public static final Map<Member, PetLab> MEMBER_PET_LAB_MAP = new HashMap<>();
    public static final Map<Long, Map<Long, Pet>> MESSAGE_PET_STATUS_MAP = new HashMap<>();
    public static final Map<Long, Pet> MESSAGE_PET_STATUS_MAP_SELECTED = new HashMap<>();


//    public static final Map<Long, User> USER_SELECTIONS = new HashMap<>();
//    public static final Map<Long, Map<Long, Pair<Pet, net.dv8tion.jda.api.entities.User>>> PET_CACHE = new HashMap<>();
//    public static final Map<Long, User> SELECTED_PET_CACHE = new HashMap<>();
//    public static final Map<Member, Map<Pet, Long>> LAST_TIME_CACHE = new HashMap<>();

    public static Database create(
        UserRepo userRepo,
        PetRepo petRepo,
        PetUserRDBMSRepository petUserRDBMSRepository,
        Config config
    ) {
        return new Database(userRepo, petRepo, petUserRDBMSRepository, config);
    }

    /*public Pet getPet(long userId) {
        return USER_SELECTIONS.get(userId).getPet();
    }*/

    /*public long insertNewPet(Long userId) {
        Pet pet = getPet(userId);
        return petRepo.save(pet).getId();
    }

    public void addPetToOwner(long userId, long guildId, long pet_id) {
        PetUserRDBMS petUserRDBMS = PetUserRDBMS.create();
        petUserRDBMS.setUserId(userId);
        petUserRDBMS.setGuildId(guildId);
        petUserRDBMS.setPetId(pet_id);
        petUserRDBMSRepository.save(petUserRDBMS);
    }*/

    @Getter @Setter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class PetLab {
        private ScheduledFuture<?> task;
        private Pet pet;
    }
}
