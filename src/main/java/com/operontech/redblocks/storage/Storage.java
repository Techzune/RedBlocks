package com.operontech.redblocks.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.block.Block;

import com.operontech.redblocks.ConsoleConnection;
import com.operontech.redblocks.RedBlocksMain;

public class Storage {
	private RedBlocksMain plugin;
	private ConsoleConnection console;
	private InventorySerializer invSerializer;
	private HashMap<String, RedBlockAnimated> rbSorted = new HashMap<String, RedBlockAnimated>();

	public Storage(final RedBlocksMain plugin) {
		this.plugin = plugin;
		console = plugin.getConsoleConnection();
		invSerializer = new InventorySerializer();
		loadRedBlocks();
	}

	/**
	 * Saves RedBlocks then clears RAM usage of the storage.
	 */
	public void clearRAMUsage() {
		saveRedBlocks();
		plugin = null;
		console = null;
		invSerializer = null;
		rbSorted = null;
	}

	/**
	 * Loads the RedBlocks.dat File in the RedBlocks Plugin Folder.
	 * If the file is using the old rbList (Set) format, it will be converted to the rbSorted (HashMap) format.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void loadRedBlocks() {
		ObjectInputStream blocksReader = null;
		try {
			final File blocks = new File(plugin.getDataFolder() + File.separator + "redblocks.dat");
			if (blocks.exists() && (blocks.length() != 0)) {
				blocksReader = new ObjectInputStream(new FileInputStream(blocks));
				final Object readObject = blocksReader.readObject();
				if (readObject instanceof HashMap<?, ?>) {
					rbSorted = (HashMap<String, RedBlockAnimated>) readObject;
					console.info("RedBlocks Loaded Successfully!");
				} else {
					rbSorted = convertSetToHashMap((Set<RedBlockAnimated>) readObject);
					console.info("RedBlocks Converted and Loaded Successfully!");
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while loading the RedBlocks file.");
		}
		try {
			if (blocksReader != null) {
				blocksReader.close();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while closing the RedBlocks file input stream.");
		}
		if (rbSorted == null) {
			rbSorted = new HashMap<String, RedBlockAnimated>();
		}
	}

	/**
	 * Saves the HashSet of RedBlocks to a file.
	 * @return if saving was successful
	 */
	public boolean saveRedBlocks() {
		ObjectOutputStream blocksWriter = null;
		try {
			final File blocks = new File(plugin.getDataFolder() + File.separator + "redblocks.dat");
			blocks.createNewFile();
			blocksWriter = new ObjectOutputStream(new FileOutputStream(blocks));
			blocksWriter.writeObject(rbSorted);
			blocksWriter.flush();
			blocksWriter.close();
			console.info("RedBlocks Saved Successfully!");
			return true;
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while saving the RedBlocks file.");
		}
		try {
			if (blocksWriter != null) {
				blocksWriter.close();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			console.warning("An error occured while closing the RedBlocks file output stream.");
		}
		return false;
	}

	/**
	 * Gets the database of RedBlocks.
	 * @return the HashMap (Key: Location String, Value: RedBlockAnimated) of RedBlocks
	 */
	public HashMap<String, RedBlockAnimated> getRedBlocks() {
		return rbSorted;
	}

	/**
	 * Searches the sorted RedBlockAnimated list for a RedBlockAnimated with a matching location.
	 *
	 * @param b the block to search for
	 * @return the RedBlockAnimated (if it was found)
	 */
	public RedBlockAnimated getRedBlock(final Block b) {
		return rbSorted.get(b.getLocation().toString());
	}

	/**
	 * Creates a RedBlockAnimated and adds it to the database of RedBlocks.
	 *
	 * @param p the owner of the RedBlock
	 * @param b the block of the RedBlock
	 * @return the created RedBlock
	 */
	public RedBlockAnimated createRedBlock(final String p, final Block b) {
		final RedBlockAnimated rb = new RedBlockAnimated(b, p, true, false);
		rbSorted.put(rb.getLocation().toString(), rb);
		return rb;
	}

	/**
	 * Removes a RedBlockAnimated from the database of RedBlocks.
	 *
	 * @param b the block to remove
	 * @return if it was removed
	 */
	public boolean removeRedBlock(final Block b) {
		return (rbSorted.remove(b.getLocation().toString()) != null);
	}

	/**
	 * Checks if the database contains a RedBlockAnimated.
	 * 
	 * @param b the block to search for
	 * @return if the RedBlockAnimated was found
	 */
	public boolean containsRedBlock(final Block b) {
		return (rbSorted.containsKey(b.getLocation().toString()));
	}

	/**
	 * Searches for the RedBlockAnimated in control of that block.
	 * @param b the block to search for the RedBlockAnimated of
	 * @return the RedBlockAnimated that owns it (if found)
	 */
	public RedBlockAnimated getRedBlockParent(final Block b) {
		final Collection<RedBlockAnimated> rbc = rbSorted.values();
		for (final RedBlockAnimated rb : rbc) {
			if (rb.contains(b)) {
				return rb;
			}
		}
		return null;
	}

	/**
	 * Runs a cleaning process that removes clutter in the RedBlockAnimated database, then resorts the HashMap.
	 * 
	 * Removes empty RedBlocks.
	 * Removes children of the RedBlockAnimated if it they're AIR.
	 * 
	 */
	public void cleanupRedBlocks() {
		console.info("Cleaning up the RedBlocks database...");
		int redBlocksRemoved = 0;
		int blocksRemoved = 0;
		List<RedBlockChild> list;
		Set<RedBlockChild> children;
		for (final Entry<String, RedBlockAnimated> entry : rbSorted.entrySet()) {
			if (!plugin.isBeingEdited(entry.getValue())) {
				if (entry.getValue().getBlockCount() == 0) {
					rbSorted.remove(entry.getKey());
					redBlocksRemoved++;
				} else {
					list = new ArrayList<RedBlockChild>();
					children = entry.getValue().getBlocks();
					for (final RedBlockChild child : children) {
						if (child.getTypeId() == 0) {
							list.add(child);
						}
					}
					blocksRemoved += entry.getValue().removeChildList(list);
				}
			}
		}
		if ((redBlocksRemoved != 0) || (blocksRemoved != 0)) {
			console.info("  " + redBlocksRemoved + " RedBlocks were removed!");
			console.info("  " + blocksRemoved + " blocks controlled by RedBlocks were removed!");
		}
	}

	/**
	 * Gets the InventorySerializer.
	 *
	 * @return the InventorySerializer
	 */
	public InventorySerializer getInventorySerializer() {
		return invSerializer;
	}

	private HashMap<String, RedBlockAnimated> convertSetToHashMap(final Set<RedBlockAnimated> oldSet) {
		final HashMap<String, RedBlockAnimated> newSet = new HashMap<String, RedBlockAnimated>();
		for (final RedBlockAnimated rb : oldSet) {
			newSet.put(rb.getLocation().toString(), rb);
		}
		return newSet;
	}
}
