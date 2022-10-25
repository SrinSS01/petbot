package me.srin.petbot.database;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Random;

@Entity
@Table(name = "pets")
@RequiredArgsConstructor
@Getter @Setter
public class Pet implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;
    String name;
    String type;
    @Column(columnDefinition = "int default 0")
    int level = 0;
    @Column(columnDefinition = "int default 0")
    int xp = 0;
    @Column(columnDefinition = "int default 100")
    int xpLimit = 100;
    int totalXp;
    String pfp;
    long cooldown = 0;
    long trainingChannelId = 0;
    @Column(columnDefinition = "bigint default 2")
    long trainingCount = 2;

    @Transient
    protected static final Random RANDOM = new Random();
    @Transient
    private int rank;

    public static Pet create() {
        return new Pet();
    }

    public void train() {
        xp++;
        totalXp++;
        if (xp >= xpLimit) {
            xp = xpLimit - xp;
            level++;
            xpLimit = level * 231;
            trainingCount *= 2;
        }
    }
}
