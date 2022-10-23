package me.srin.petbot;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
public class PetbotApplication {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PetbotApplication.class);
    public static void main(String[] args) throws IOException {
        File config = new File("config");
        if (!config.exists()) {
            boolean result = config.mkdirs();
            File properties = new File("config/application.yml");
            if (result) {
                LOGGER.info("Created config directory");
                try (FileWriter writer = new FileWriter(properties)) {
                    writer.write("""
                            bot:
                              token: token
                              training-cooldown-in-seconds: 720
                              pet-species: ["cat","dog","parrot","hamster","snake","dragon","fox","deer","squirrel","beast"]
                              status-background: null
                              status-color: yellow
                              training-channels:
                                - channel id 1
                                - channel id 2
                              
                            database:
                              host: "localhost:3306"
                              name: "name"
                              user: "user"
                              password: "password"
                            """);
                    writer.flush();
                    LOGGER.info("Created application.yml file");
                }
            } else {
                LOGGER.error("Failed to create config directory");
            }
            return;
        }
        SpringApplication.run(PetbotApplication.class, args);
    }
}
