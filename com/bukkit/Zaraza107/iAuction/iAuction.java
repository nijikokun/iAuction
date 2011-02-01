package com.bukkit.Zaraza107.iAuction;

import com.nijiko.permissions.PermissionHandler;
import java.io.*;
import java.util.HashMap;
import java.lang.String;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.*;
import com.nijikokun.bukkit.iConomy.iConomy;
import com.nijikokun.bukkit.iConomy.Messaging;
import com.nijikokun.bukkit.Permissions.Permissions;
import java.util.Map;

/**
 * iAuction for iConomy for Bukkit
 *
 * @author Zaraza107
 */
public class iAuction extends JavaPlugin {

    private final iAuctionPlayerListener playerListener = new iAuctionPlayerListener(this);
    private final iAuctionBlockListener blockListener = new iAuctionBlockListener(this);
    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    public static Server server;
    public String currency = "";
    private Timer auctionTimer;
    private TimerTask auctionTT;
    public boolean isAuction = false;
    public int auction_time;
    public Player auction_owner;
    public Player winner;
    private int auction_item_id;
    private short auction_item_damage;
    private int auction_item_byte;
    private String auction_item_name;
    private int auction_item_amount;
    private int auction_item_starting;
    private int auction_item_bid;
    private boolean win = false;
    private int currentBid;
    public Player timer_player;
    public String tag = "&e[AUCTION] ";
    public iConomy iConomy;
    public PermissionHandler Permissions;

    /*
     * Internal Properties controllers
     */
    public static iProperty Item;
    public static HashMap<String, String> items;

    public iAuction(PluginLoader pluginLoader, Server instance,
            PluginDescriptionFile desc, File folder, File plugin,
            ClassLoader cLoader) throws IOException {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
        // TODO: Place any custom initialization code here

        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
        server = instance;
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

        // Register our events
        // EXAMPLE: Custom code, here we just output some info so we can check all is well

        PluginManager pm = server.getPluginManager();
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " (version " + pdfFile.getVersion() + ") is enabled!");

        pm.registerEvent(org.bukkit.event.Event.Type.PLAYER_COMMAND, ((Listener) (playerListener)), Priority.Monitor, ((Plugin) (this)));

        Plugin ic = server.getPluginManager().getPlugin("iConomy");
        if (ic == null) {
            System.out.println("Warning! iConomy plugin is not loaded!");
        } else {
            iConomy = (iConomy)ic;
            currency = this.iConomy.currency;
        }

        Item = new iProperty("items.db");
        setupItems();
    }

    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
        // EXAMPLE: Custom code, here we just output some info so we can check all is well

        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " (version " + pdfFile.getVersion() + ") is disabled.");
    }

    /**
     * Setup Items
     */
    public void setupItems() {
        Map mappedItems = null;
        items = new HashMap<String, String>();

        try {
            mappedItems = Item.returnMap();
        } catch (Exception ex) {
            System.out.println(Messaging.bracketize("iAuction Flatfile") + " could not open items.db!");
        }

        if (mappedItems != null) {
            for (Object item : mappedItems.keySet()) {
                String left = (String) item;
                String right = (String) mappedItems.get(item);
                String id = left.trim();
                String itemName;
                //log.info("Found " + left + "=" + right + " in items.db");
                if (id.matches("[0-9]+") || id.matches("[0-9]+,[0-9]+")) {
                    //log.info("matches");
                    if (right.contains(",")) {
                        String[] synonyms = right.split(",");
                        itemName = synonyms[0].replaceAll("\\s", "");
                        items.put(id, itemName);
                        //log.info("Added " + id + "=" + itemName);
                        for (int i = 1; i < synonyms.length; i++) {
                            itemName = synonyms[i].replaceAll("\\s", "");
                            items.put(itemName, id);
                            //log.info("Added " + itemName + "=" + id);
                        }
                    } else {
                        itemName = right.replaceAll("\\s", "");
                        items.put(id, itemName);
                        //log.info("Added " + id + "=" + itemName);
                    }
                } else {
                    itemName = left.replaceAll("\\s", "");
                    id = right.trim();
                    items.put(itemName, id);
                    //log.info("Added " + itemName + "=" + id);
                }
            }
        }
    }

    public void setupPermissions() {
        Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

        if (this.Permissions == null) {
            if (test != null) {
                this.Permissions = ((Permissions)test).getHandler();
            } else {
                System.out.println(Messaging.bracketize("iAuction") + " Permission system not enabled. Disabling plugin.");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }

    public void auctionStart(Player player, int time, int id, short damage, int data, int amount, int price) {
        auction_time = time;
        auction_owner = player;
        timer_player = player;
        auction_item_id = id;
        auction_item_damage = damage;
        auction_item_byte = data;
        auction_item_amount = amount;
        auction_item_starting = price;
        currentBid = price;
        ItemStack stack = new ItemStack(id, amount, damage, (byte)data);
        auction_item_name = stack.getType().name();
        PlayerInventory inventory = player.getInventory();

        if (auctionCheck(player, inventory, id, data, amount, time, price)) {
            isAuction = true;
            inventory.removeItem(new ItemStack[]{new ItemStack(id, amount)});

            final Server serv = server;
            final int interval = auction_time;
            final iAuction plug = this;

            auctionTT = new TimerTask() {

                int i = interval;
                Server sr = serv;
                double half = java.lang.Math.floor(i / 2);
                iAuction pl = plug;

                @Override
                public void run() {
                    if (half <= 10) {
                        if (i == interval || i == 10 || i == 3 || i == 2) {
                            Messaging.broadcast(tag + i + " seconds left to bid!");
                        }
                    } else {
                        if (i == interval || i == half || i == 10 || i == 3 || i == 2) {
                             Messaging.broadcast(tag + i + " seconds left to bid!");
                        }
                    }
                    if (i == 1) {
                        Messaging.broadcast(tag + i + " seconds left to bid!");
                    }
                    if (i == 0) {
                        pl.auctionStop(timer_player);
                    }
                    i--;
                }
            };
            Messaging.broadcast(tag + "Auction Started!");
            auctionInfo(server, null);

            auctionTimer = new Timer();
            auctionTimer.scheduleAtFixedRate(auctionTT, 0L, 1000L);
        }
    }

    public boolean auctionCheck(Player player, PlayerInventory inventory, int id, int data, int amount, int time, int price) {
        if (!isAuction) {
            if (time > 10) {
                ItemStack[] stacks = inventory.getContents();
                int size = 0;
                for (int i = 0; i < stacks.length; i++) {
                    if (stacks[i].getTypeId() == id) {
                        size += stacks[i].getAmount();
                    }
                }
                if (amount <= size) {
                    if (price >= 0) {
                        return true;
                    } else {
                        Messaging.send(player, tag + " &7The starting price has to be at least 0!");
                        return false;
                    }
                } else {
                    Messaging.send(player, tag + "&7You don't have enough " + Items.name(id, data) + " to do that!");
                    return false;
                }
            } else {
                Messaging.send(player, tag + "&7Time must be longer than 10 seconds!");
                return false;
            }
        } else {
            Messaging.send(player, tag + "&7There is already an auction running!");
            return false;
        }
    }

    public void auctionInfo(Server server, Player player) {
        if (server != null) {
            Messaging.broadcast(tag + "Auctioned Item: &f" + Items.name(auction_item_id, auction_item_byte) + " &e[&f"+auction_item_id+"&e]");
            Messaging.broadcast(tag + "Amount: &f" + auction_item_amount);
            Messaging.broadcast(tag + "Starting Price: &f" + auction_item_starting);
            Messaging.broadcast(tag + "Owner: &f" + auction_owner.getDisplayName());
        }
        if (player != null) {
            Messaging.send(player, tag + "Auctioned Item: &f" + Items.name(auction_item_id, auction_item_byte) + " &e[&f"+auction_item_id+"&e]");
            Messaging.send(player, tag + "Amount: &f" + auction_item_amount);
            Messaging.send(player, tag + "Starting Price: &f" + auction_item_starting);
            Messaging.send(player, tag + "Owner: &f" + auction_owner.getDisplayName());
        }
    }

    public void auctionStop(Player player) {
        if (!isAuction) {
            Messaging.send(player, tag + "&7There is no auction currently running!");
        } else {
            isAuction = false;
            auctionTimer.cancel();

            if (win) {
                PlayerInventory winv = winner.getInventory();

                Messaging.broadcast("&2 -- Auction Ended -- Winner [ &f" + winner.getDisplayName() + "&2 ] -- ");
                Messaging.send(winner, tag + "&fEnjoy your items!");
                Messaging.send(auction_owner, tag + "&fYour items have been sold for " + currentBid + " " + this.iConomy.currency + "!");

                int balance = this.iConomy.db.get_balance(winner.getName());
                balance -= currentBid;
                this.iConomy.db.set_balance(winner.getName(), balance);
                balance = this.iConomy.db.get_balance(auction_owner.getName());
                balance += currentBid;
                this.iConomy.db.set_balance(auction_owner.getName(), balance);
                winv.addItem(new ItemStack[]{new ItemStack(auction_item_id, auction_item_amount, auction_item_damage, (byte)auction_item_byte)});
            } else {
                Messaging.broadcast("&2 -- Auction ended with no bids --");
                Messaging.send(auction_owner, tag + "&fYour items have been returned to you!");
                auction_owner.getInventory().addItem(new ItemStack[]{new ItemStack(auction_item_id, auction_item_amount, auction_item_damage, (byte)auction_item_byte)});
            }

            auction_item_id = 0;
            auction_item_amount = 0;
            auction_item_starting = 0;
            currentBid = 0;
            auction_item_bid = 0;
            auction_item_name = null;
            winner = null;
            auction_owner = null;
            win = false;
        }
    }

    public void auctionBid(Player player, int bid, int sbid) {
        String name = player.getName();

        if (isAuction) {
            if (bid > currentBid) {
                win = true;

                if (bid > auction_item_bid) {
                    currentBid = bid;
                    auction_item_bid = sbid;
                    winner = player;
                    Messaging.broadcast(tag + "Bid raised to &f" + bid + " " + this.iConomy.currency + "&e by &f" + player.getDisplayName());
                } else {
                    Messaging.broadcast(tag + "&7You have been outbid by &8" + winner.getDisplayName() + "'s&7 secret bid!");
                    Messaging.broadcast(tag + "Bid raised! Currently stands at: " + bid + " " + this.iConomy.currency );
                    currentBid = bid + 1;

                }
            } else {
                Messaging.send(player, tag + "&7Your bid was too low.");
            }
        } else {
            Messaging.send(player, tag + "&7There is no auction running at the moment.");
        }
    }
}
