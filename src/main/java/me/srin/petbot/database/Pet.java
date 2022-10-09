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
public class Pet implements Serializable {
    @Id @Getter @Setter
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;
    @Getter @Setter
    String name;

    @Getter @Setter
    String type;

    @Getter @Setter @Column(columnDefinition = "int default 0")
    int level = 0;

    @Getter @Setter @Column(columnDefinition = "int default 0")
    int xp = 0;

    @Getter @Setter @Column(columnDefinition = "int default 100")
    int xpLimit = 100;

    @Getter @Setter
    String pfp;

    @Transient
    protected static final Random RANDOM = new Random();

    public static Pet create() {
        return new Pet();
    }

    protected static int generateXP() {
        return RANDOM.nextInt(10);
    }

    /**
     * @return true if leveled up
     * */
    public boolean train() {
        int dXP = generateXP();
        xp += dXP;
        if (xp >= xpLimit) {
            xp = xpLimit - xp;
            level++;
            xpLimit = level * 201;
            return true;
        }
        return false;
    }
}