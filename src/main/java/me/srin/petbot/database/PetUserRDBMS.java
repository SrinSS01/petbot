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
    @Id private long memberId;
    @Id private long guildId;
    @Id private long petId;

    public static PetUserRDBMS create() {
        return new PetUserRDBMS();
    }

    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter @Setter
    public static class ID implements Serializable {
        private long memberId;
        private long guildId;
        private long petId;
    }
}
