package com.operontech.redblocks.playerdependent;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.operontech.redblocks.storage.RedBlockAnimated;

public class PlayerSession implements Serializable {
	private static final long serialVersionUID = 1L;
	private final UUID p;
	private RedBlockAnimated rb;
	private Block rbBlock;

	private Double enableDelay;
	private Double disableDelay;

	/**
	 * Stores player data for the RedBlock
	 * @param playerUUID the UUID of the player
	 * @param redblock the RedBlock currently being controlled by the player
	 * @param block the block of the RedBlock currently being controlled by the player
	 */
	public PlayerSession(final UUID playerUUID, final RedBlockAnimated redblock, final Block block) {
		p = playerUUID;
		rb = redblock;
		rbBlock = block;
		enableDelay = 0D;
		disableDelay = 0D;
	}

	public void setEnableDelay(final Double placeDelay) {
		this.enableDelay = placeDelay;
	}

	public void setDisableDelay(final Double breakDelay) {
		this.disableDelay = breakDelay;
	}

	public void setRedBlock(final RedBlockAnimated redblock, final Block block) {
		rb = redblock;
		rbBlock = block;
	}

	public Double getEnableDelay() {
		return enableDelay;
	}

	public Double getDisableDelay() {
		return disableDelay;
	}

	public Player getPlayer() {
		return Bukkit.getServer().getPlayer(p);
	}

	public UUID getUUID() {
		return p;
	}

	public boolean isEditingRedBlock() {
		return rb == null;
	}

	public RedBlockAnimated getRedBlock() {
		return rb;
	}

	public Block getBlock() {
		return rbBlock;
	}
}