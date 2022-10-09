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
public class PetUserRDBMS implements Serializable {
    @Getter @Setter @Id
    private long userId;
    @Getter @Setter @Id
    private long guildId;

    @Getter @Setter @Id
    private long petId;

    public static PetUserRDBMS create() {
        return new PetUserRDBMS();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ID implements Serializable {
        @Getter @Setter
        private long userId;
        @Getter @Setter
        private long guildId;
        @Getter @Setter
        private long petId;

        public static ID of(long user_id, long guild_id, long petId) {
            return new ID(user_id, guild_id, petId);
        }
    }
}
