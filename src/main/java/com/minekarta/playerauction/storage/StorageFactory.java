package com.minekarta.playerauction.storage;

import com.minekarta.playerauction.storage.json.JsonAuctionStorage;
import com.minekarta.playerauction.storage.json.JsonTransactionStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class StorageFactory {

    public static AuctionStorage createAuctionStorage(JavaPlugin plugin) {
        return new JsonAuctionStorage(plugin);
    }

    public static TransactionStorage createTransactionStorage(JavaPlugin plugin) {
        return new JsonTransactionStorage(plugin);
    }
}

