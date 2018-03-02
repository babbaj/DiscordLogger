package com.babbaj;

import com.babbaj.utils.Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Babbaj on 2/23/2018.
 */
public class DeletedMessageListener extends ListenerAdapter {

    private static final File DELETED_DIRECTORY = new File("deleted");
    private static final File DELETED_EMBEDS_DIRECTORY = new File(DELETED_DIRECTORY, "embeds");
    private static final File CACHE_DIRECTORY = new File("cache");
    private static final File EMBED_DIRECTORY = new File("embed_cache");
    private static final File TEXT_OUTPUT = new File("log.txt");
    static {
        DELETED_DIRECTORY.mkdir();
        CACHE_DIRECTORY.mkdir();
        EMBED_DIRECTORY.mkdir();
        DELETED_EMBEDS_DIRECTORY.mkdir();
        try {
            if (!TEXT_OUTPUT.exists())
                TEXT_OUTPUT.createNewFile();
        } catch (IOException e) {
            System.err.println("Failed to create log file");
            e.printStackTrace();
        }
    }

    public static final List<String> GUILD_BLACKLIST = Arrays.asList(

    );

    public static final List<String> CHANNEL_BLACKLIST = Arrays.asList(

    );

    private final Cache<String, Message> MESSAGE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        Message message = MESSAGE_CACHE.getIfPresent(event.getMessageId());
        if (message == null) return;
        String out = deleteEventToString(event, message);
        System.out.println(out);

        try {
            Files.write(TEXT_OUTPUT.toPath(), out.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to file");
            e.printStackTrace();
        }
        message.getAttachments().forEach(attachment -> {
            File cached = findFile(getFileName(message, attachment), CACHE_DIRECTORY);
            if (cached != null) {
                cached.renameTo(new File(DELETED_DIRECTORY, cached.getName()));
            }
        });

        message.getEmbeds().stream()
                .filter(embed -> embed.getThumbnail() != null)
                .map (MessageEmbed::getThumbnail)
                .forEach(thumbnail -> {
                    final String fileName = getFileName(message, thumbnail.getProxyUrl());
                    File cached = findFile(fileName, EMBED_DIRECTORY);
                    if (cached != null) {
                        cached.renameTo(new File(DELETED_EMBEDS_DIRECTORY, cached.getName()));
                    }
                });

        MESSAGE_CACHE.invalidate(message.getId());
    }

    private static String deleteEventToString(GuildMessageDeleteEvent event, Message message) {
        String text = message.getContentDisplay();

        StringBuilder out = new StringBuilder();

        out.append(String.format("%s - %s - %s",
                event.getGuild().getName(),
                event.getChannel().getName(),
                message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator()));
        out.append("\r\n");

        if (!text.isEmpty()) {
            out.append(text);
            out.append("\r\n");
        }

        if (message.getAttachments().size() > 0) {
            out.append(message.getAttachments().size() + " attachments");
            out.append("\r\n");
        }
        out.append("\r\n");

        return out.toString();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // ignore bot messages
        if (CHANNEL_BLACKLIST.contains(event.getChannel().getName())) return;
        if (event.getGuild() != null &&
            GUILD_BLACKLIST.contains(event.getGuild().getName())) return;

        Message message = event.getMessage();

        message.getAttachments().forEach(attachment -> {
            String fileName = getFileName(message, attachment);
            File file = new File(CACHE_DIRECTORY, fileName);
            if (file.exists()) {
                try {
                    byte[] data = Utils.downloadFile(attachment.getUrl());
                    if (data != null) {
                        Files.write(file.toPath(), data, StandardOpenOption.CREATE_NEW);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        message.getEmbeds().stream()
                .filter(embed -> embed.getThumbnail() != null)
                .forEach(embed -> {
                    final String url = embed.getThumbnail().getProxyUrl();
                    final String fileName = getFileName(message, url);

                    File file = new File(EMBED_DIRECTORY, fileName);
                    try {
                        byte[] data = Utils.downloadFile(url);
                        if (data != null) {
                            Files.write(file.toPath(), data, StandardOpenOption.CREATE_NEW);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        MESSAGE_CACHE.put(message.getId(), message);
    }

    private static File findFile(String name, File directory) {
        if (!directory.isDirectory()) throw new IllegalArgumentException("File is not a directory");
        File[] files = directory.listFiles(file -> name.equals(file.getName()));
        if (files != null && files.length > 0)
            return files[0];
        return null;
    }

    private static String getFileName(Message message, Message.Attachment attachment) {
        return String.format("%s-%s", message.getAuthor().getName(), attachment.getFileName());
    }

    private static String getFileName(Message message, String url) {
        return String.format("%s-%s",message.getAuthor().getName(), url.substring(url.lastIndexOf('/') + 1));
    }

}
