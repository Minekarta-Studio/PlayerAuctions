package com.minekarta.playerauction.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.auction.model.AuctionStatus;
import com.minekarta.playerauction.common.SerializedItem;
import com.minekarta.playerauction.gui.model.AuctionCategory;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.storage.AuctionStorage;
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
import java.util.stream.IntStream;

public class JsonAuctionStorage implements AuctionStorage {
    private final JavaPlugin plugin;
    private final Executor executor;
    private final String filePath;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // In-memory cache for performance
    private volatile List<Auction> auctions = new ArrayList<>();
    
    public JsonAuctionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "JsonAuctionStorage-Thread");
            t.setDaemon(true);
            return t;
        });
        this.filePath = plugin.getDataFolder().getAbsolutePath() + "/auctions.json";
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
        
        plugin.getLogger().info("JSON auction storage initialized successfully.");
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<Auction>>(){}.getType();
                    List<Auction> loadedAuctions = gson.fromJson(reader, listType);
                    if (loadedAuctions != null) {
                        this.auctions = new ArrayList<>(loadedAuctions);
                    } else {
                        this.auctions = new ArrayList<>();
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to load auctions from JSON file: " + e.getMessage());
                    this.auctions = new ArrayList<>();
                }
            } else {
                this.auctions = new ArrayList<>();
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
                gson.toJson(auctions, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write auctions to temporary file: " + e.getMessage());
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
    public CompletableFuture<Optional<Auction>> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctions.stream()
                    .filter(auction -> auction.id().equals(id))
                    .findFirst();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findActive(int limit, int offset, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                List<Auction> activeAuctions = auctions.stream()
                    .filter(auction -> auction.status() == AuctionStatus.ACTIVE)
                    .filter(auction -> category == AuctionCategory.ALL ||
                           category.matches(auction.item().toItemStack().getType().name()))
                    .filter(auction -> searchQuery == null || searchQuery.trim().isEmpty() || 
                           matchesSearch(auction, searchQuery.toLowerCase()))
                    .sorted(getComparator(sortOrder))
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
                
                return activeAuctions;
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findActiveAuctions(int page, int limit, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return findActive(limit, (page - 1) * limit, category, sortOrder, searchQuery);
    }

    @Override
    public CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctions.stream()
                    .filter(auction -> auction.seller().equals(seller))
                    .sorted((a1, a2) -> Long.compare(a2.createdAt(), a1.createdAt())) // Sort by newest first
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findPlayerHistory(UUID playerId, int page, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctions.stream()
                    .filter(auction -> auction.seller().equals(playerId))
                    .filter(auction -> auction.status() != AuctionStatus.ACTIVE)
                    .sorted((a1, a2) -> Long.compare(a2.createdAt(), a1.createdAt())) // Sort by newest first
                    .skip((page - 1) * limit)
                    .limit(limit)
                    .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countActiveBySeller(UUID sellerId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return (int) auctions.stream()
                    .filter(auction -> auction.seller().equals(sellerId))
                    .filter(auction -> auction.status() == AuctionStatus.ACTIVE)
                    .count();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countActiveAuctionsByPlayer(UUID playerId) {
        return countActiveBySeller(playerId);
    }

    @Override
    public CompletableFuture<Integer> countAllActiveAuctions() {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return (int) auctions.stream()
                    .filter(auction -> auction.status() == AuctionStatus.ACTIVE)
                    .count();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> insertAuction(Auction a) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctions.add(a);
                saveData();
            } finally {
                lock.writeLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> updateAuctionIfVersionMatches(Auction a, int expectedVersion) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                // Find the auction with the same ID
                OptionalInt indexOpt = IntStream.range(0, auctions.size())
                    .filter(i -> auctions.get(i).id().equals(a.id()))
                    .findFirst();
                
                if (indexOpt.isPresent()) {
                    int index = indexOpt.getAsInt();
                    Auction existing = auctions.get(index);
                    
                    // Check if version matches
                    if (existing.version() == expectedVersion) {
                        auctions.set(index, a);
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
    public CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctions.stream()
                    .filter(auction -> auction.status() == AuctionStatus.ACTIVE)
                    .filter(auction -> auction.endAt() <= nowEpochMillis)
                    .limit(batchSize)
                    .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countActiveAuctions(AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                final String normalizedQuery = (searchQuery == null) ? null : searchQuery.trim().toLowerCase();
                return (int) auctions.stream()
                    .filter(auction -> auction.status() == AuctionStatus.ACTIVE)
                    .filter(auction -> category == AuctionCategory.ALL ||
                        category.matches(auction.item().toItemStack().getType().name()))
                    .filter(auction -> normalizedQuery == null || normalizedQuery.isEmpty() || matchesSearch(auction, normalizedQuery))
                    .count();
            } finally {
                lock.readLock().unlock();
            }
        }, executor);
    }

    private boolean matchesSearch(Auction auction, String searchQuery) {
        String itemType = auction.item().toItemStack().getType().name().toLowerCase();
        String itemName = auction.item().toItemStack().hasItemMeta() && 
                         auction.item().toItemStack().getItemMeta().hasDisplayName() ?
                         auction.item().toItemStack().getItemMeta().getDisplayName().toLowerCase() : "";
        
        return itemType.contains(searchQuery) || itemName.contains(searchQuery);
    }

    private Comparator<Auction> getComparator(SortOrder sortOrder) {
        switch (sortOrder) {
            case PRICE_ASC:
                return Comparator.comparing(Auction::price);
            case PRICE_DESC:
                return Comparator.comparing(Auction::price).reversed();
            case NEWEST:
                return Comparator.comparing(Auction::createdAt).reversed();
            case TIME_LEFT:
            default:
                return Comparator.comparing(Auction::endAt);
        }
    }
}

