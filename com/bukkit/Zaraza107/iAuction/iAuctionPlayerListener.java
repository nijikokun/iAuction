package com.bukkit.Zaraza107.iAuction;

import com.nijikokun.bukkit.iConomy.Messaging;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import com.nijikokun.bukkit.iConomy.iConomy;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 * Handle events for all Player related events
 * @author Zaraza107
 */
public class iAuctionPlayerListener extends PlayerListener {

    private final iAuction plugin;

    public iAuctionPlayerListener(iAuction instance) {
        plugin = instance;
    }

    public void help() {
        Messaging.send("&e -----[ &fAuction Help&e ]----- ");
        Messaging.send("&f/auction start&e|&f? - Returns this");
        Messaging.send("&f/auction start&e|&f-s <&etime&f> <&eitem&f> <&eamount&f> <&estarting price&f>");
        Messaging.send("&e- Starts an auction for &e<&ftime&e> seconds with &e<&famount&e> of");
        Messaging.send("&e- &e<&fitem&e> for &e<&fstarting price&e>.");
        Messaging.send("&f/auction bid&e|&f-b <&ebid&f> (&emaximum bid&f)");
        Messaging.send("&e- If you set a &e(&fmaximum bid&e) and a &e<&fbid&e> is greater than the");
        Messaging.send("&e- current, you will outbid that bid if it is lower than your maximum.");
        Messaging.send("&f/auction info&e|&f-i - Returns auction information");

    }

    public void onPlayerCommand(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String msg[] = event.getMessage().split(" ");
        Messaging.save(player);

        if (msg[0].equalsIgnoreCase("/auction")) {
            if (msg.length < 2) {
                help();
                return;
            } else {

                if (msg[1].equalsIgnoreCase("help") || msg[1].equalsIgnoreCase("?")) {
                    help(); return;
                }

                if (msg[1].equalsIgnoreCase("start") || msg[1].equalsIgnoreCase("-s")) {
                    if(msg.length < 6) {
                        Messaging.send(plugin.tag + "&7Invalid syntax.");
                        Messaging.send("&f/auction start&e|&f-s <&etime&f> <&eitem&f> <&eamount&f> <&estarting price&f>");
                        return;
                    }

                    int time = Integer.parseInt(msg[2]);
                    int[] id = new int[]{-1, 0};
                    short dura = 0;
                    int count = 0;
                    int amount = 0;
                    int price = 0;
                    
                    try {
                        amount = Integer.parseInt(msg[4]);
                        price = Integer.parseInt(msg[5]);
                    } catch (NumberFormatException e) {
                        Messaging.send(plugin.tag + "&7Invalid amount and price."); return;
                    }

                    id = Items.validate(msg[3]);

                    if (id[0] == -1 || id[0] == 0) {
                        Messaging.send(plugin.tag + "&7Invalid item id."); return;
                    }

                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item.getTypeId() == id[0]) {
                            MaterialData data = item.getData();

                            if(id[1] != 0) {
                                if (data.getData() == (byte)id[1]) {
                                    dura = item.getDurability();
                                }
                            } else {
                                dura = item.getDurability();
                            }
                        }
                    }

                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item.getTypeId() == id[0]) {
                            MaterialData data = item.getData();
                            
                            if (id[1] != 0) {
                                if (data.getData() == (byte)id[1]) {
                                    if(item.getDurability() == dura) {
                                        count++;
                                    }
                                }
                            } else {
                                if(item.getDurability() == dura) {
                                    count++;
                                }
                            }
                        }
                    }
                    
                    if (amount < count) {
                        Messaging.send(plugin.tag + "&7Sorry but you only have &8" + count + "&7 of that item."); return;
                    }

                    plugin.auctionStart(player, time, id[0], dura, id[1], amount, price);
                }

                if (msg[1].equalsIgnoreCase("end") || msg[1].equalsIgnoreCase("-e")) {
                    if (plugin.Permissions.has(player, "auction.end") || player == plugin.auction_owner) {
                        plugin.auctionStop(player);
                    } else {
                        Messaging.send(plugin.tag + "&7Sorry, you cannot end this auction.");
                    }
                }

                if (msg[1].equalsIgnoreCase("bid") || msg[1].equalsIgnoreCase("-b")) {
                    if (player != plugin.auction_owner) {
                        int bid = Integer.parseInt(msg[2]);
                        int balance = iConomy.db.get_balance(name);

                        if (bid <= balance) {
                            if (msg.length == 3) {
                                plugin.auctionBid(player, bid, 0);
                            }

                            if (msg.length == 4) {
                                int sbid = Integer.parseInt(msg[3]);

                                if (sbid <= balance) {
                                    plugin.auctionBid(player, bid, sbid);
                                } else {
                                    Messaging.send(plugin.tag + "&7You don't have enough &8" + plugin.iConomy.currency + "&7 to do that.");
                                }
                            }
                        } else {
                            Messaging.send(plugin.tag + "&7You don't have enough &8" + plugin.iConomy.currency + "&7 to do that.");
                        }
                    } else {
                        Messaging.send(plugin.tag + "&7You cannot bid on your own auction!");
                    }
                }

                if (msg[1].equalsIgnoreCase("info") || msg[1].equalsIgnoreCase("-i")) {
                    if (plugin.isAuction) {
                        Messaging.send("&e -----[ &fAuction Information&e ]-----");
                        plugin.auctionInfo(null, player);

                        if (plugin.winner != null) {
                            Messaging.send(plugin.tag + "Current Winner: &f" + plugin.winner.getDisplayName());
                        }
                    } else {
                        Messaging.send(plugin.tag + "&7No auctions in session at the moment!");
                    }
                }
            }
        }
    }
}
