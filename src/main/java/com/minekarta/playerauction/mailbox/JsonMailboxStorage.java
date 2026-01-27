package com.minekarta.playerauction.mailbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minekarta.playerauction.mailbox.model.MailboxItem;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * JSON-based implementation of MailboxStorage
 */
public class JsonMailboxStorage implements MailboxStorage {

    private final JavaPlugin plugin;
    private final Executor executor;
    private final String filePath;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // In-memory cache
    private volatile List<MailboxItem> mailboxItems = new ArrayList<>();

    public JsonMailboxStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MailboxStorage-Thread");
            t.setDaemon(true);
            return t;
        });
        this.filePath = plugin.getDataFolder().getAbsolutePath() + "/mailbox.json";
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }

    @Override
    public void init() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadData();
        plugin.getLogger().info("JSON mailbox storage initialized successfully.");
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<MailboxItem>>(){}.getType();
                    List<MailboxItem> loaded = gson.fromJson(reader, listType);
                    if (loaded != null) {
                        this.mailboxItems = new ArrayList<>(loaded);
                    } else {
                        this.mailboxItems = new ArrayList<>();
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to load mailbox from JSON: " + e.getMessage());
                    this.mailboxItems = new ArrayList<>();
                }
            } else {
                this.mailboxItems = new ArrayList<>();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveData() {
        lock.readLock().lock();
        try {
            File file = new File(filePath);
            File tempFile = new File(filePath + ".tmp");

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                gson.toJson(mailboxItems, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write mailbox to temporary file: " + e.getMessage());
                return;
            }

            // Atomic move with retry logic
            int retries = 3;
            boolean renamed = false;

            while (retries > 0 && !renamed) {
                if (file.exists()) {
                    if (!file.delete()) {
                        plugin.getLogger().warning("Failed to delete old mailbox file (attempt " + (4 - retries) + ")");
                    }
                }

                renamed = tempFile.renameTo(file);

                if (!renamed) {
                    retries--;
                    if (retries > 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            plugin.getLogger().warning("Interrupted while waiting to retry mailbox file rename");
                            break;
                        }
                    }
                }
            }

            if (!renamed) {
                plugin.getLogger().severe("Failed to rename temporary mailbox file after 3 attempts!");
                if (tempFile.exists()) {
                    plugin.getLogger().severe("Temporary file still exists at: " + tempFile.getAbsolutePath());
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public CompletableFuture<Void> addItem(MailboxItem item) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                mailboxItems.add(item);
                saveData();
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<MailboxItem>> getUnclaimedItems(UUID playerId, int page, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return mailboxItems.stream()
                    .filter(item -> item.playerId().equals(playerId))
                    .filter(item -> !item.claimed())
                    .filter(item -> !item.isExpired())
                    .sorted(Comparator.comparing(MailboxItem::createdAt).reversed())
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> getUnclaimedCount(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return (int) mailboxItems.stream()
                    .filter(item -> item.playerId().equals(playerId))
                    .filter(item -> !item.claimed())
                    .filter(item -> !item.isExpired())
                    .count();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> claimItem(UUID itemId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                for (int i = 0; i < mailboxItems.size(); i++) {
                    MailboxItem item = mailboxItems.get(i);
                    if (item.id().equals(itemId) && !item.claimed()) {
                        mailboxItems.set(i, item.markAsClaimed());
                        saveData();
                        return true;
                    }
                }
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<MailboxItem>> getItem(UUID itemId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return mailboxItems.stream()
                    .filter(item -> item.id().equals(itemId))
                    .findFirst();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> deleteExpiredItems() {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                int sizeBefore = mailboxItems.size();
                mailboxItems.removeIf(item -> item.isExpired() && !item.claimed());
                int deleted = sizeBefore - mailboxItems.size();

                if (deleted > 0) {
                    saveData();
                    plugin.getLogger().info("Deleted " + deleted + " expired mailbox items");
                }

                return deleted;
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> deleteOldClaimedItems(int days) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
                int sizeBefore = mailboxItems.size();

                mailboxItems.removeIf(item -> item.claimed() && item.createdAt() < cutoffTime);

                int deleted = sizeBefore - mailboxItems.size();

                if (deleted > 0) {
                    saveData();
                    plugin.getLogger().info("Deleted " + deleted + " old claimed mailbox items");
                }

                return deleted;
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }
}
