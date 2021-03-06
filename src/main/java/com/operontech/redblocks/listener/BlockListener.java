package com.operontech.redblocks.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.operontech.redblocks.ConfigValue;
import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.RedBlocksMain;
import com.operontech.redblocks.playerdependent.Permission;
import com.operontech.redblocks.storage.RedBlockAnimated;

@SuppressWarnings("deprecation")
public class BlockListener implements Listener {
	private final RedBlocksMain plugin;

	public BlockListener(final RedBlocksMain plugin) {
		this.plugin = plugin;
	}

	/**
	 * Initiates an interaction event for blocks (Block Break and Damage)
	 * @param p the player of the event
	 * @param b the block of the event
	 * @return if the event was cancelled
	 */
	private boolean blockBreakDamaged(final Player p, final Block b, final boolean broken) {
		plugin.doBlockUpdate(b);
		final boolean isRedBlockAnimated = plugin.getStorage().containsRedBlock(b);
		if (isRedBlockAnimated && p.isSneaking() && (p.getItemInHand().getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_destroyItem))) {
			// Destroy RedBlock
			if (Permission.CREATEANDDESTROY.check(p)) {
				return !plugin.destroyRedBlock(b, p);
			} else {
				ConsoleConnection.error(p, "You do not have permissions to destroy RedBlocks!");
				return false;
			}
		}
		if (plugin.isEditing(p)) {

			// Stop Editing a RedBlock
			if (isRedBlockAnimated) {
				if (!plugin.getRedBlockEditing(p).getLocation().toString().equals(b.getLocation().toString())) {
					ConsoleConnection.error(p, "You are already editing a RedBlock!", "Say " + ChatColor.GOLD + "/rb s" + ChatColor.RED + " to stop editing.");
					return true;
				}
				plugin.removeEditor(p);
				return true;
			}

			// Verify RedBlockAnimated TypeID Integrity and Remove RedBlock
			final Block controlledRB = plugin.getRedBlockEditing(p).getBlock();
			if (controlledRB.isEmpty()) {
				controlledRB.setTypeId(plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID));
			}
			if (plugin.getRedBlockEditing(p).contains(b) && broken) {
				plugin.removeBlock(p, plugin.getRedBlockEditing(p), b);
				return false;
			}

		} else {

			RedBlockAnimated parent = null;
			if (plugin.isActiveBlock(b)) {
				parent = plugin.getStorage().getRedBlockParent(b);
			}

			//  Verify Protection of Block
			if ((parent != null) && !Permission.BYPASS_PROTECT.check(p)) {
				if (parent.getOptionProtected()) {
					ConsoleConnection.error(p, "That block is protected by a RedBlock!");
					return true;
				}
			}

			// Begin Editing of RedBlockAnimated and Create RedBlockAnimated (If Necessary)
			if (isRedBlockAnimated) {
				if (Permission.USE.check(p)) {
					plugin.addEditor(p, b);
					return true;
				} else {
					ConsoleConnection.error(p, "You don't have the permissions to edit RedBlocks!");
					return true;
				}
			} else if ((b.getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) && (b.getRelative(BlockFace.UP).getType() == Material.REDSTONE_WIRE)) {
				if (Permission.CREATEANDDESTROY.check(p)) {
					if (parent == null) {
						plugin.createRedBlock(p, b);
						return true;
					} else {
						ConsoleConnection.error(p, "That block is controlled by another RedBlock!");
						return true;
					}
				} else {
					ConsoleConnection.error(p, "You don't have the permissions to create RedBlocks!");
					return false;
				}
			}
		}
		return false;
	}

	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		plugin.doBlockUpdate(event.getBlock());
		if (plugin.isEditing(event.getPlayer())) {
			final RedBlockAnimated rb = plugin.getRedBlockEditing(event.getPlayer());
			if (rb.getBlock().isEmpty()) {
				rb.getBlock().setTypeId(plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID));
			}
			if ((rb.getBlockCount() > plugin.getConfiguration().getInt(ConfigValue.rules_maxBlocksPer)) && !Permission.BYPASS_MAXBLOCKSPER.check(event.getPlayer())) {
				ConsoleConnection.error(event.getPlayer(), "You can't add anymore blocks! The maximum is: " + plugin.getConfiguration().getString(ConfigValue.rules_maxBlocksPer) + " Blocks");
				event.setCancelled(true);
			} else if (rb.contains(event.getBlock())) {
				ConsoleConnection.error(event.getPlayer(), "There's a block here already!");
				event.setCancelled(true);
			} else {
				plugin.addBlock(event.getPlayer(), rb, event.getBlock(), true);
			}
		}
	}

	@EventHandler
	public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
		if (plugin.isEditing(event.getPlayer())) {
			final RedBlockAnimated rb = plugin.getRedBlockEditing(event.getPlayer());
			if (rb.getBlock().isEmpty()) {
				rb.getBlock().setTypeId(plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID));
			}
			if ((rb.getBlockCount() > plugin.getConfiguration().getInt(ConfigValue.rules_maxBlocksPer)) && !Permission.BYPASS_MAXBLOCKSPER.check(event.getPlayer())) {
				ConsoleConnection.error(event.getPlayer(), "You can't add anymore blocks! The maximum is: " + plugin.getConfiguration().getString(ConfigValue.rules_maxBlocksPer) + " Blocks");
				event.setCancelled(true);
			} else {
				plugin.addBlock(event.getPlayer(), rb, event.getBlockClicked().getRelative(event.getBlockFace()), true);
			}
		}
	}

	@EventHandler
	public void onBucketFill(final PlayerBucketFillEvent event) {
		if (plugin.isActiveBlock(event.getBlockClicked().getRelative(event.getBlockFace()))) {
			if (plugin.isEditing(event.getPlayer())) {
				plugin.removeBlock(event.getPlayer(), plugin.getRedBlockEditing(event.getPlayer()), event.getBlockClicked().getRelative(event.getBlockFace()));
			} else {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		event.setCancelled(blockBreakDamaged(event.getPlayer(), event.getBlock(), true));
	}

	@EventHandler
	public void onBlockDamage(final BlockDamageEvent event) {
		event.setCancelled(blockBreakDamaged(event.getPlayer(), event.getBlock(), false));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if ((event.hasBlock()) && (event.getClickedBlock().getType() == Material.FIRE) && (event.getAction() == Action.LEFT_CLICK_BLOCK) && plugin.isEditing(event.getPlayer())) {
			final RedBlockAnimated rb = plugin.getRedBlockEditing(event.getPlayer());
			if ((rb != null) && rb.contains(event.getClickedBlock())) {
				rb.remove(event.getClickedBlock());
			}
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (plugin.isEditing(event.getPlayer())) {
						final RedBlockAnimated rb = plugin.getRedBlockEditing(event.getPlayer());
						if (rb.contains(event.getClickedBlock()) && (rb.getChild(event.getClickedBlock()).getData() != event.getClickedBlock().getData())) {
							plugin.updateBlock(event.getPlayer(), plugin.getRedBlockEditing(event.getPlayer()), event.getClickedBlock());
						}
					}
				}
			}
		}, 2L);
	}

	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
		if ((e.getEntityType() == EntityType.FALLING_BLOCK) && plugin.isActiveBlock(e.getBlock())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPhysics(final BlockPhysicsEvent e) {
		if (e.getBlock().getTypeId() == plugin.getConfiguration().getInt(ConfigValue.redblocks_blockID)) {
			plugin.doBlockUpdate(e.getBlock());
		}
	}

	@EventHandler
	public void onPistonExtend(final BlockPistonExtendEvent e) {
		for (final Block b : e.getBlocks()) {
			if (plugin.isActiveBlock(b)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPistonPull(final BlockPistonRetractEvent e) {
		if (e.isSticky() && !e.getRetractLocation().getBlock().isEmpty() && (plugin.isActiveBlock((e.getRetractLocation().getBlock())))) {
			e.setCancelled(true);
		}
	}
}