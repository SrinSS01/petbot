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
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SlashCommand extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommand.class);

    private SlashCommand(Database database) {
        super(database);
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
        if (!selfMember.hasPermission(event.getGuildChannel(), Permission.VIEW_CHANNEL)) {
            event.reply("I do not have permission `VIEW_CHANNEL`").setEphemeral(true).queue();
            return;
        }
        String command = event.getName();
        User user = event.getUser();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        long guildId = event.getGuild().getIdLong();
        switch (command) {
            case "create-pet" -> {
                event.deferReply().queue();
                InteractionHook hook = event.getHook();
                Database.PetLab petLab = new Database.PetLab();
                Pet pet = Pet.create();
                pet.setType(Objects.requireNonNull(event.getOption("type")).getAsString());
                petLab.setPet(pet);
                long memberId = member.getIdLong();
                Database.MEMBER_PET_LAB_MAP.computeIfAbsent(guildId, k -> new HashMap<>());
                Database.MEMBER_PET_LAB_MAP.get(guildId).put(memberId, petLab);

                EmbedBuilder petStatsEmbed = Utils.getPetDetails(pet);
                petStatsEmbed.setFooter(Date.from(Instant.now()).toString(), user.getAvatarUrl());
                MessageEmbed embed = petStatsEmbed.build();
                hook.editOriginalEmbeds(embed)
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("change-name", "Change name"),
                                        Button.primary("set-pfp", "Set pfp"),
                                        Button.success("save", "Save Changes"),
                                        Button.danger("remove", "Remove")
                                )
                        )
                        .queue(message -> message.editMessageComponents(
                                ActionRow.of(
                                        Button.primary("change-name", "Change name").asDisabled(),
                                        Button.primary("set-pfp", "Set pfp").asDisabled(),
                                        Button.success("save", "Save Changes").asDisabled(),
                                        Button.danger("remove", "Remove").asDisabled()
                                )
                        ).queueAfter(3, TimeUnit.MINUTES));
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
                    Database.MEMBER_PET_LAB_MAP.computeIfAbsent(guildId, k -> Map.of());
                    Database.MEMBER_PET_LAB_MAP.get(guildId).put(member.getIdLong(), petLab);
                    petLab.setPet(pet);
                    Member finalMember = member;
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
                                ).queueAfter(3, TimeUnit.MINUTES, m -> Database.MEMBER_PET_LAB_MAP.get(guildId).remove(finalMember.getIdLong()));
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
                String warning = "⚠";
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
                Optional<PetUserRDBMS> optionalPetUser = database.getPetUserRDBMSRepository().findByGuildIdAndPetId(guildId, pet_id);
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
                    entity.setGuildId(guildId);
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
                List<Long> petIds = database.getPetUserRDBMSRepository().findByMemberIdAndGuildId(memberIdLong, guildId);
                if (petIds.isEmpty()) {
                    event.replyFormat("%s doesn't own any pets", member.getAsMention()).setEphemeral(true).queue();
                    return;
                }
                event.deferReply().queue();
                Pet unsavedPetData = Database.PET_MAP.get(petIds.get(0));
                if (unsavedPetData != null) {
                    database.getPetRepo().save(unsavedPetData);
                }
                List<Pet> pets = database.getPetRepo().findAllById(petIds);
                StringSelectMenu.Builder builder = StringSelectMenu.create("pet-stats");
                for (Pet pet : pets) {
                    String petName = pet.getName();
                    builder.addOption(petName, String.valueOf(pet.getId()), pet.getType());
                }
                SelectMenu selectionMenu = builder.build();
                Pet pet = pets.get(0);
                long petId = pet.getId();

                int rank = database.getPetRepo().getRankById(petId);
                pet.setRank(rank);
                var petStats = Utils.getPetStats(pet, database.getConfig());
                Request request = new Request.Builder()
                        .url(petStats)
                        .method("GET", null)
                        .build();
                try (Response response = Utils.client.newCall(request).execute()) {
                    if (response.body() == null) {
                        return;
                    }
                    byte[] bytes = response.body().bytes();
                    FileUpload fileUpload = FileUpload.fromData(bytes, "stats.png");
                    if (!isModerator && database.getConfig().getTrainingChannels().contains(event.getChannel().getIdLong())) {
                        event.getHook().sendFiles(fileUpload).setComponents(
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
                                .sendFiles(fileUpload)
                                .setComponents(ActionRow.of(selectionMenu))
                                .queue(message -> {
                                    Database.MESSAGE_PET_STATUS_MAP_SELECTED.put(message.getIdLong(), pet.getId());
                                    message.editMessageComponents(
                                            ActionRow.of(selectionMenu.asDisabled())
                                    ).queueAfter(10, TimeUnit.MINUTES, m -> Database.MESSAGE_PET_STATUS_MAP_SELECTED.remove(m.getIdLong()));
                                });
                    }
                } catch (IOException ignored) {}
            }
            case "start-training" -> {
                if (!database.getConfig().getTrainingChannels().contains(event.getChannel().getIdLong())) {
                    event.reply("Train your pet in the training channel").setEphemeral(true).queue();
                    return;
                }
                long petId = Objects.requireNonNull(event.getOption("pet")).getAsLong();
                Optional<PetUserRDBMS> petUser = database.getPetUserRDBMSRepository().findById(PetUserRDBMS.ID.of(member.getIdLong(), guildId, petId));
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
                        event.replyFormat(
                                ":stopwatch: Oh your pet is still training in <#%d>, wait until you can use this command again at <t:%d:T>",
                                        pet.getTrainingChannelId(), cooldown + trainingCooldownInSeconds
                                )
                                .setEphemeral(true).queue();
                    } else {
                        long xpLimit = pet.getXpLimit();
                        long trainingCount = pet.getTrainingCount();
                        long periodInSeconds = (trainingCooldownInSeconds * 1000) / (xpLimit / trainingCount);

                        pet.setCooldown(currentTime);
                        pet.setTrainingChannelId(event.getChannel().getIdLong());
                        ScheduledFuture<?> scheduledFuture = Utils.EXECUTOR.scheduleWithFixedDelay(
                                pet::train, 0, periodInSeconds, TimeUnit.MILLISECONDS
                        );
                        database.getPetRepo().save(pet);
                        Database.PET_MAP.put(pet.getId(), pet);
                        event.replyFormat("pet started training in %s", event.getChannel().getAsMention()).setEphemeral(true).queue();
                        Utils.EXECUTOR.schedule(() -> {
                            database.getPetRepo().save(pet);
                            Database.PET_MAP.remove(pet.getId());
                            scheduledFuture.cancel(true);
                        }, trainingCooldownInSeconds, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }
}
