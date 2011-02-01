package com.bukkit.Zaraza107.iAuction;

import org.bukkit.block.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.*;
import org.bukkit.inventory.Inventory;

/**
 * iAuction block listener
 * @author Zaraza107
 */
public class iAuctionBlockListener extends BlockListener {
    private final iAuction plugin;
    public String stuff ="";

    public iAuctionBlockListener(final iAuction plugin) {
        this.plugin = plugin;
    }
    
   
    
}
