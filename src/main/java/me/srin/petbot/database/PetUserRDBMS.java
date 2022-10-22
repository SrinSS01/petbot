package me.srin.petbot.database;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@IdClass(PetUserRDBMS.ID.class)
@Table(name = "up_db")
@NoArgsConstructor
@Getter @Setter
public class PetUserRDBMS implements Serializable {
    @Id private long userId;
    @Id private long guildId;
    @Id private long petId;

    public static PetUserRDBMS create() {
        return new PetUserRDBMS();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter @Setter
    public static class ID implements Serializable {
        private long userId;
        private long guildId;
        private long petId;

        public static ID of(long user_id, long guild_id, long petId) {
            return new ID(user_id, guild_id, petId);
        }
    }
}
