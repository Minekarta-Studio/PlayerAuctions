package com.minekarta.playerauction.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minekarta.playerauction.storage.TransactionStorage;
import com.minekarta.playerauction.transaction.model.Transaction;
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

public class JsonTransactionStorage implements TransactionStorage {
    private final JavaPlugin plugin;
    private final Executor executor;
    private final String filePath;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // In-memory cache for performance
    private volatile List<Transaction> transactions = new ArrayList<>();
    
    public JsonTransactionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "JsonTransactionStorage-Thread");
            t.setDaemon(true);
            return t;
        });
        this.filePath = plugin.getDataFolder().getAbsolutePath() + "/transactions.json";
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        
        // Load existing data on initialization
        loadData();
    }

    @Override
    public void init() {
        // Create data directory if it doesn't exist
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Load existing data
        loadData();
        
        plugin.getLogger().info("JSON transaction storage initialized successfully.");
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<Transaction>>(){}.getType();
                    List<Transaction> loadedTransactions = gson.fromJson(reader, listType);
                    if (loadedTransactions != null) {
                        this.transactions = new ArrayList<>(loadedTransactions);
                    } else {
                        this.transactions = new ArrayList<>();
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to load transactions from JSON file: " + e.getMessage());
                    this.transactions = new ArrayList<>();
                }
            } else {
                this.transactions = new ArrayList<>();
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
                gson.toJson(transactions, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write transactions to temporary file: " + e.getMessage());
                return;
            }
            
            // Atomic move to prevent corruption
            if (!tempFile.renameTo(file)) {
                plugin.getLogger().severe("Failed to rename temporary file to final file");
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public CompletableFuture<Void> logTransaction(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                transactions.add(transaction);
                saveData();
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Transaction>> findTransactionsByPlayer(UUID playerId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return transactions.stream()
                    .filter(transaction -> 
                        (transaction.actorUuid() != null && transaction.actorUuid().equals(playerId)) ||
                        (transaction.sellerUuid() != null && transaction.sellerUuid().equals(playerId)))
                    .sorted((t1, t2) -> Long.compare(t2.timestamp(), t1.timestamp())) // Sort by newest first
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }
}