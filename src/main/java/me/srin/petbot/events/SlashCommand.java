package me.srin.petbot.events;

import me.srin.petbot.database.Database;
import me.srin.petbot.database.Pet;
import me.srin.petbot.database.PetUserRDBMS;
import me.srin.petbot.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.srin.petbot.utils.Utils.SELECTION_BUILDER;

public class SlashCommand extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommand.class);

    private SlashCommand(Database database) {
        super(database);
        for (String type : database.getConfig().getPetSpecies()) {
            SELECTION_BUILDER.addOption(type, type);
        }
    }

    public static SlashCommand create(Database database) {
        return new SlashCommand(database);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Member selfMember = guild.getMemberById(event.getJDA().getSelfUser().getId());
        if (selfMember == null) {
            return;
        }
        if (!selfMember.hasPermission(Permission.VIEW_CHANNEL)) {
            event.reply("I do not have permission `VIEW_CHANNEL`").setEphemeral(true).queue();
            return;
        }
        String command = event.getName();
        User user = event.getUser();
//        long user_id = user.getIdLong();
        long guild_id = guild.getIdLong();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        switch (command) {
            case "create-pet" -> {
                event.deferReply().queue();
                InteractionHook hook = event.getHook();
                Utils.createPet(hook, member);
            }
            case "edit-pet" -> {
                event.deferReply().queue();
                int petId = Objects.requireNonNull(event.getOption("id")).getAsInt();
                Optional<Pet> optionalPet = database.getPetRepo().findById((long) petId);
                if (optionalPet.isPresent()) {
                    Pet pet = optionalPet.get();
                    EmbedBuilder petStatsEmbed = Utils.getPetDetails(pet);
                    petStatsEmbed.setFooter(Date.from(Instant.now()).toString(), user.getAvatarUrl());
                    MessageEmbed embed = petStatsEmbed.build();
                    Database.PetLab petLab = new Database.PetLab();
                    Database.MEMBER_PET_LAB_MAP.put(member, petLab);
                    petLab.setPet(pet);
                    event.getHook()
                            .editOriginalEmbeds(embed)
                            .setComponents(
                                    ActionRow.of(
                                            Button.primary("change-name", "Change name"),
                                            Button.primary("set-pfp", "Set pfp"),
                                            Button.success("save", "Save Changes"),
                                            Button.danger("remove", "Remove")
                                    )
                            )
                            .queue(message -> {
                                LOGGER.info("message id: {}", message.getId());
                                message.editMessageComponents(
                                        ActionRow.of(
                                                Button.primary("change-name", "Change name").asDisabled(),
                                                Button.primary("set-pfp", "Set pfp").asDisabled(),
                                                Button.success("save", "Save Changes").asDisabled(),
                                                Button.danger("remove", "Remove").asDisabled()
                                        )
                                ).queueAfter(3, TimeUnit.MINUTES, m -> Database.MEMBER_PET_LAB_MAP.remove(m.getMember()));
                            });
                } else {
                    event.getHook()
                            .setEphemeral(true)
                            .editOriginalFormat("pet with id: %d not found", petId)
                            .queue();
                }
            }
            case "pet-list" -> {
                event.deferReply().queue();
                List<Pet> petList = database.getPetRepo().findAll();
                if (petList.isEmpty()) {
                    event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setDescription("Pet list empty :(")
                            .setColor(0xFFFF0000)
                            .build()
                    ).queue();
                    return;
                }
                StringBuilder petDetails = new StringBuilder();
                String idCol = "%4s|";
                String typeCol = "%15s|";
                String nameCol = "%15s";
                petDetails
                        .append("```\n")
                        .append(idCol.formatted("id"))
                        .append(typeCol.formatted("type"))
                        .append(nameCol.formatted("name")).append('\n')
                        .append("-".repeat(4)).append('+').append("-".repeat(15)).append('+').append("-".repeat(15));
                petList.forEach(pet -> {
                    String name = pet.getName();
                    petDetails.append("\n")
                            .append(idCol.formatted(pet.getId()))
                            .append(typeCol.formatted(pet.getType()))
                            .append(nameCol.formatted(name == null? "unknown": name));
                });
                petDetails.append("\n```");
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("**Pet list**")
                        .setDescription(petDetails)
                        .setColor(Utils.randomColor())
                        .build()).queue();
            }
            case "assign-pet" -> {
                // unicode for warning sign
                String warning = "\u26A0";
                event.deferReply(true).queue();
                int pet_id = Objects.requireNonNull(event.getOption("id")).getAsInt();
                boolean petExists = database.getPetRepo().existsById((long) pet_id);
                if (!petExists) {
                    event.getHook()
                            .editOriginalEmbeds(
                                    new EmbedBuilder()
                                            .setTitle(warning + " Error")
                                            .setDescription("pet with id: %d not found".formatted(pet_id))
                                            .setColor(0xFFFF0000)
                                            .build()
                            )
                            .queue();
                    return;
                }
                User asUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                Optional<PetUserRDBMS> optionalPetUser = database.getPetUserRDBMSRepository().findByPetId(pet_id);
                if (optionalPetUser.isPresent()) {
                    PetUserRDBMS petUser = optionalPetUser.get();
                    event.getHook()
                            .editOriginalEmbeds(
                                    new EmbedBuilder()
                                            .setTitle(warning + " Error")
                                            .setDescription("Sorry but this pet has already been assigned to <@%s>".formatted(petUser.getUserId()))
                                            .setColor(0xFFFF0000)
                                            .build()
                            )
                            .queue();
                } else {
                    PetUserRDBMS entity = PetUserRDBMS.create();
                    entity.setGuildId(guild_id);
                    entity.setPetId(pet_id);
                    entity.setUserId(asUser.getIdLong());
                    database.getPetUserRDBMSRepository().save(entity);
                    event.getHook()
                            .editOriginalEmbeds(new EmbedBuilder()
                                    .setTitle("Success")
                                    .setDescription("Pet with id: %d has been assigned to %s".formatted(pet_id, asUser.getAsMention()))
                                    .setColor(0xFF00FF00)
                                    .build()
                            )
                            .queue();
                }
            }
            /*case "view-stats" -> {
                var usr = database.getUserRepo().findById(me.srin.petbot.database.User.ID.of(user_id, guild_id));
                if (usr.isEmpty()) {
                    event.reply("who are you?").setEphemeral(true).queue();
                    return;
                }
                var petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(user_id, guild_id);
                if (petIds.isEmpty()) {
                    event.reply("You don't have any pets").setEphemeral(true).queue();
                    return;
                }
                var pets = database.getPetRepo().findAllById(petIds);
                StringBuilder sb = new StringBuilder();
                pets.forEach(pet ->
                        sb.append('`').append(pet.getName()).append('`').append('\n')
                          .append(Utils.getProgressBar(pet.getXp(), pet.getXpLimit())).append("\n\n"));
                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("**User stats**")
                        .setThumbnail(user.getAvatarUrl())
                        .setDescription(
                                """
                                `user:` %s
                                `xp:` (%d/%d)
                                `level`: %d
                                
                                _progress_
                                %s
                                                            
                                _pets progress_
                                %s
                                """.formatted(
                                        user.getAsTag(),
                                        usr.get().getXp(), usr.get().getXpLimit(),
                                        usr.get().getLevel(),
                                        Utils.getProgressBar(usr.get().getXp(), usr.get().getXpLimit()),
                                        sb.toString()
                                )
                        ).setTimestamp(new Date().toInstant()).build();
                event.replyEmbeds(embed).queue();
            }*/
            case "pet-stats", "view-pets-user" -> {
                OptionMapping option = event.getOption("user");
                boolean isModerator = false;
                if (option != null) {
                    member = option.getAsMember();
                    if (member == null) {
                        event.reply("User not found").setEphemeral(true).queue();
                        return;
                    }
                    isModerator = true;
                }
                List<Long> petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(member.getUser().getIdLong(), guild_id);
                if (petIds.isEmpty()) {
                    event.reply("You don't own any pets").setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                SelectMenu.Builder builder = SelectMenu.create("pet-stats");
                HashMap<Long, Pet> petMap = new HashMap<>();
                for (Pet pet : pets) {
                    petMap.put(pet.getId(), pet);
                    String petName = pet.getName();
                    builder.addOption(petName, String.valueOf(pet.getId()), pet.getType());
                }
                SelectMenu selectionMenu = builder.build();
                Pet pet = pets.get(0);
                var petStats = Utils.getPetStats(pet, database.getConfig().getStatusBackground());
                if (!isModerator) {
                    event.getHook().editOriginal(petStats).setComponents(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(
                                    Button.primary("train-pet", "Train")
                            )
                    ).queue(message -> {
                        Database.MEMBER_PET_STATUS_MAP.put(message.getIdLong(), petMap);
                        message.editMessageComponents(
                                ActionRow.of(selectionMenu.asDisabled()),
                                ActionRow.of(
                                        Button.primary("train-pet", "Train").asDisabled()
                                )
                        ).queueAfter(10, TimeUnit.MINUTES, m -> Database.MEMBER_PET_STATUS_MAP.remove(m.getIdLong()));
                    });
                } else {
                    event.getHook()
                            .editOriginal(petStats)
                            .setComponents(ActionRow.of(selectionMenu))
                            .queue(message -> {
                                Database.MEMBER_PET_STATUS_MAP.put(message.getIdLong(), petMap);
                                message.editMessageComponents(
                                        ActionRow.of(selectionMenu.asDisabled())
                                ).queueAfter(10, TimeUnit.MINUTES, m -> Database.MEMBER_PET_STATUS_MAP.remove(m.getIdLong()));
                            });
                }
            }
            /*case "start-training" -> {
                OptionMapping petOption = event.getOption("pet");
                if (petOption == null) {
                    return;
                }
                String petName = petOption.getAsString();
                var petIds = database.getPetUserRDBMSRepository().findByUserIdAndGuildId(user_id, guild_id);
                var usr = database.getUserRepo().findById(me.srin.petbot.database.User.ID.of(user_id, guild_id));
                if (petIds.isEmpty()) {
                    event.reply("You don't own any pets").setEphemeral(true).queue();
                    return;
                }
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                Optional<Pet> optionalPet = pets.stream().filter(pet -> pet.getName().equals(petName)).findAny();
                if (optionalPet.isPresent() && usr.isPresent()) {
                    Pet pet = optionalPet.get();
                    Map<Pet, Long> map = Database.LAST_TIME_CACHE.get(member);
                    Long lastTime = map == null? 0L: Objects.requireNonNullElse(map.get(pet), 0L);
                    long dTime = System.currentTimeMillis() - lastTime;
                    if (dTime <= database.getConfig().getTrainingCooldownInSeconds()) {
                        event.replyFormat("Oh your pet is still training, wait for <t:%d:T> until you can use this command again", dTime).setEphemeral(true).queue();
                        return;
                    }
                    boolean isLevelup = pet.train();
                    if (isLevelup) {
                        usr.get().levelUp();
                    }
                    database.getPetRepo().save(pet);
                    Database.LAST_TIME_CACHE.put(member, Map.of(pet, System.currentTimeMillis() / 1000));
                } else {
                    event.reply("You don't own any pets named " + petName).setEphemeral(true).queue();
                }
            }*/
        }
    }
}
