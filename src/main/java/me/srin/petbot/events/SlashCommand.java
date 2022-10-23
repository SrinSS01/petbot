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
import java.util.concurrent.ScheduledFuture;
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
                Member asMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
                if (asMember == null) {
                    return;
                }
                Optional<PetUserRDBMS> optionalPetUser = database.getPetUserRDBMSRepository().findByPetId(pet_id);
                if (optionalPetUser.isPresent()) {
                    PetUserRDBMS petUser = optionalPetUser.get();
                    event.getHook()
                            .editOriginalEmbeds(
                                    new EmbedBuilder()
                                            .setTitle(warning + " Error")
                                            .setDescription("Sorry but this pet has already been assigned to <@%s>".formatted(petUser.getMemberId()))
                                            .setColor(0xFFFF0000)
                                            .build()
                            )
                            .queue();
                } else {
                    PetUserRDBMS entity = PetUserRDBMS.create();
                    entity.setMemberId(asMember.getIdLong());
                    entity.setPetId(pet_id);
                    database.getPetUserRDBMSRepository().save(entity);
                    event.getHook()
                            .editOriginalEmbeds(new EmbedBuilder()
                                    .setTitle("Success")
                                    .setDescription("Pet with id: %d has been assigned to %s".formatted(pet_id, asMember.getAsMention()))
                                    .setColor(0xFF00FF00)
                                    .build()
                            )
                            .queue();
                }
            }
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
                long memberIdLong = member.getIdLong();
                List<Long> petIds = database.getPetUserRDBMSRepository().findByMemberId(memberIdLong);
                if (petIds.isEmpty()) {
                    event.reply("You don't own any pets").setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                SelectMenu.Builder builder = SelectMenu.create("pet-stats");
                for (Pet pet : pets) {
                    String petName = pet.getName();
                    builder.addOption(petName, String.valueOf(pet.getId()), pet.getType());
                }
                SelectMenu selectionMenu = builder.build();
                Pet pet = pets.get(0);
                long petId = pet.getId();
                int rank = database.getPetRepo().getRankById(petId);
                LOGGER.info("id: {} rank = {}", petId, rank);
                pet.setRank(rank);
                var petStats = Utils.getPetStats(pet, database.getConfig());
                if (!isModerator && database.getConfig().getTrainingChannels().contains(event.getChannel().getIdLong())) {
                    event.getHook().editOriginal(petStats).setComponents(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(
                                    Button.primary("train-pet", "Train")
                            )
                    ).queue(message -> {
                        Database.MESSAGE_PET_STATUS_MAP_SELECTED.put(message.getIdLong(), pet.getId());
                        message.editMessageComponents(
                                ActionRow.of(selectionMenu.asDisabled()),
                                ActionRow.of(
                                        Button.primary("train-pet", "Train").asDisabled()
                                )
                        ).queueAfter(10, TimeUnit.MINUTES, m -> Database.MESSAGE_PET_STATUS_MAP_SELECTED.remove(m.getIdLong()));
                    });
                } else {
                    event.getHook()
                            .editOriginal(petStats)
                            .setComponents(ActionRow.of(selectionMenu))
                            .queue(message -> {
                                Database.MESSAGE_PET_STATUS_MAP_SELECTED.put(message.getIdLong(), pet.getId());
                                message.editMessageComponents(
                                        ActionRow.of(selectionMenu.asDisabled())
                                ).queueAfter(10, TimeUnit.MINUTES, m -> Database.MESSAGE_PET_STATUS_MAP_SELECTED.remove(m.getIdLong()));
                            });
                }
            }
            case "start-training" -> {
                if (!database.getConfig().getTrainingChannels().contains(event.getChannel().getIdLong())) {
                    event.reply("Train your pet in the training channel").setEphemeral(true).queue();
                    return;
                }
                long petId = Objects.requireNonNull(event.getOption("pet")).getAsLong();
                Optional<PetUserRDBMS> petUser = database.getPetUserRDBMSRepository().findById(PetUserRDBMS.ID.of(member.getIdLong(), petId));
                if (petUser.isEmpty()) {
                    event.reply("You don't own any pets by id " + petId).setEphemeral(true).queue();
                    return;
                }
                Optional<Pet> optionalPet = database.getPetRepo().findById(petId);
                if (optionalPet.isPresent()) {
                    Pet pet = optionalPet.get();
                    long cooldown = pet.getCooldown();
                    long currentTime = System.currentTimeMillis() / 1000;
                    long trainingCooldownInSeconds = database.getConfig().getTrainingCooldownInSeconds();
                    if ((currentTime - cooldown) <= trainingCooldownInSeconds) {
                        event.replyFormat(":stopwatch: Oh your pet is still training, wait until you can use this command again at <t:%d:T>", cooldown + trainingCooldownInSeconds)
                                .setEphemeral(true).queue();
                    } else {
                        long xpLimit = pet.getXpLimit();
                        long trainingCount = pet.getTrainingCount();
                        long periodInSeconds = (trainingCooldownInSeconds * 1000) / (xpLimit / trainingCount);

                        pet.setCooldown(currentTime);
                        ScheduledFuture<?> scheduledFuture = Utils.EXECUTOR.scheduleWithFixedDelay(
                                () -> {
                                    pet.train();
                                    database.getPetRepo().save(pet);
                                }, 0, periodInSeconds, TimeUnit.MILLISECONDS
                        );
                        event.replyFormat("pet started training in %s", event.getChannel().getName()).setEphemeral(true).queue();
                        Utils.EXECUTOR.schedule(() -> {
                            scheduledFuture.cancel(true);
                        }, trainingCooldownInSeconds, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }
}
