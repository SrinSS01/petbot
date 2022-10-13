package me.srin.petbot.database;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Random;

@Entity
@IdClass(User.ID.class)
@Table(name = "users")
@NoArgsConstructor
@ToString
public class User implements Serializable {
    @Id @Getter @Setter
    private long userId;
    @Id @Getter @Setter
    private long guildId;

    @Getter @Setter
    String tag;

    @Getter @Setter @Column(columnDefinition = "int default 0")
    int level = 0;

    @Getter @Setter @Column(columnDefinition = "int default 0")
    int xp = 0;

    @Getter @Setter @Column(columnDefinition = "int default 100")
    int xpLimit = 100;

    @Transient @Getter @Setter
    Pet pet;
    @Transient
    protected static final Random RANDOM = new Random();

    public static User create() {
        return new User();
    }

    public static User create(long user_id, long guild_id) {
        User user = new User();
        user.setUserId(user_id);
        user.setGuildId(guild_id);
        return user;
    }

    private static int generateXP() {
        return RANDOM.nextInt(1, 10);
    }

    public void levelUp() {
        int dXP = generateXP();
        xp += dXP;
        if (xp >= xpLimit) {
            xp = xpLimit - xp;
            level++;
            xpLimit = level * RANDOM.nextInt(201, 401);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ID implements Serializable {
        @Getter @Setter
        private long userId;
        @Getter @Setter
        private long guildId;

        public static ID of(long user_id, long guild_id) {
            return new ID(user_id, guild_id);
        }
    }
}
