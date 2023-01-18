package me.srin.petbot;

import lombok.val;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication
public class PetbotApplication {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PetbotApplication.class);
    public static void main(String[] args) throws IOException {
        val config = new File("config");
        if (!config.exists()) {
            val mkdir = config.mkdir();
            if (!mkdir) {
                LOGGER.error("Failed to create config directory");
                return;
            }
            LOGGER.info("Created config directory");
        }
        val properties = new File("config/application.yml");
        if (!properties.exists()) {
            try (
                    val is = PetbotApplication.class.getResourceAsStream("../../../application.yml")
            ) {
                if (is == null) {
                    return;
                }
                byte[] bytes = is.readAllBytes();
                String str = new String(bytes);
                Files.writeString(properties.toPath(), str.substring(0, str.indexOf("#internals")));
                LOGGER.info("Created application.yml file");
            }
            return;
        }
        SpringApplication.run(PetbotApplication.class, args);
    }
}
