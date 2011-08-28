package com.MoofIT.Minecraft.BlueTelepads;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class BlueTelepadsPlayerListener extends PlayerListener {
	private final BlueTelepads plugin;
	private static HashMap<String, Location> lapisLinks  = new HashMap<String, Location>();
	private static HashMap<String, Long> teleportTimeouts = new HashMap<String, Long>();

	public BlueTelepadsPlayerListener(BlueTelepads instance) {
		this.plugin = instance;
	}

	public static void msgPlayer(Player player,String msg) {
		player.sendMessage(ChatColor.DARK_AQUA + "[BlueTelepads] " + ChatColor.AQUA + msg);
	}

	public boolean isTelepadLapis(Block lapisBlock) {
		if (lapisBlock.getTypeId() != plugin.telepadCenterID) return false;

		//get the data val of the slab to the north to check that all slabs are the same
		short slabType = lapisBlock.getRelative(BlockFace.NORTH).getData();
		if (slabType != plugin.telepadSurroundingNormal && slabType != plugin.telepadSurroundingFree) return false;

		BlockFace[] surroundingChecks = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
		for (BlockFace check : surroundingChecks) {
			if (lapisBlock.getRelative(check).getTypeId() != 43 && lapisBlock.getRelative(check).getData() != slabType) return false;
		}

		if (lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.SIGN_POST && lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.WALL_SIGN) return false;
		if (lapisBlock.getRelative(BlockFace.UP).getType() != Material.STONE_PLATE) return false;

		return true;
	}

	public boolean isTelepadFree(Block lapisBlock) {
		if (isTelepadLapis(lapisBlock) && lapisBlock.getRelative(BlockFace.NORTH).getData() == plugin.telepadSurroundingFree) return true;
		return false; 
	}


	private String toHex(int number) {
		return Integer.toHexString(number + 32000);
	}

	private int toInt(String hex) {
		return Integer.parseInt(hex, 16) - 32000;
	}

	private Block getTelepadLapisReceiver(Block senderLapis) {
		Block senderSign = senderLapis.getRelative(BlockFace.DOWN);

		if (senderSign.getType() == Material.WALL_SIGN || senderSign.getType() == Material.SIGN_POST) {
			Sign ssenderSign = (Sign)senderSign.getState();

			String sHexLocation = ssenderSign.getLine(2);

			String sWorld = ssenderSign.getLine(1);
			String[] sXYZ = sHexLocation.split(":");

			World world = plugin.getServer().getWorld(sWorld);

			if (world == null) {
				return null;
			}
		
			Block receiverLapis = world.getBlockAt(toInt(sXYZ[0]),toInt(sXYZ[1]),toInt(sXYZ[2]));

			if (isTelepadLapis(receiverLapis)) {
				return receiverLapis;
			}
		}
		return null;
	}

	//currently assumes you checked both blocks with isTelepadLapis
	private void linkTelepadLapisReceivers(Block lapis1,Block lapis2) {
		Sign slapis1 = (Sign)lapis1.getRelative(BlockFace.DOWN).getState();
		Sign slapis2 = (Sign)lapis2.getRelative(BlockFace.DOWN).getState();

		slapis1.setLine(1,slapis2.getWorld().getName());
		slapis2.setLine(1,slapis1.getWorld().getName());

		Location lLapis1 = lapis1.getLocation();
		Location lLapis2 = lapis2.getLocation();

		slapis1.setLine(2,toHex(lLapis2.getBlockX()) + ":" + toHex(lLapis2.getBlockY()) + ":" + toHex(lLapis2.getBlockZ()));
		slapis2.setLine(2,toHex(lLapis1.getBlockX()) + ":" + toHex(lLapis1.getBlockY()) + ":" + toHex(lLapis1.getBlockZ()));

		slapis1.update(true);
		slapis2.update(true);
	}

	private static int getDistance(Location loc1,Location loc2) {
		return (int)Math.sqrt(Math.pow(loc2.getBlockX() - loc1.getBlockX(),2) + Math.pow(loc2.getBlockY() - loc1.getBlockY(),2) + Math.pow(loc2.getBlockZ() - loc1.getBlockZ(),2));
	}

	private boolean TelepadsWithinDistance(Block block1,Block block2) {
		if (plugin.maxDistance == 0) {
			return true;
		}
		if (getDistance(block1.getLocation(),block2.getLocation()) < plugin.maxDistance) {
			return true;
		}
		return false;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		//Using a telepad, note we verify the timeout here after checking if it's a telepad
		if (event.getAction() == Action.PHYSICAL
		&& event.getClickedBlock() != null
		&& isTelepadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))
		&& (!teleportTimeouts.containsKey(player.getName()) || teleportTimeouts.get(player.getName()) < System.currentTimeMillis())) {
			Block senderLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);
			Block receiverLapis = getTelepadLapisReceiver(senderLapis);

			//Verify receiver is a working telepad
			if (receiverLapis != null) {
				//Verify permissions
				if (!player.hasPermission("bluetelepads.use")) {
					msgPlayer(player,"You do not have permission to use telepads.");
					return;
				}

				//Verify distance
				if (!TelepadsWithinDistance(senderLapis,receiverLapis)) {
					msgPlayer(player,ChatColor.RED + "Error: Telepads are too far apart! (Distance:" + getDistance(senderLapis.getLocation(),receiverLapis.getLocation()) + ",MaxAllowed:" + plugin.maxDistance + ")");
					return;
				}
				boolean isFree = isTelepadFree(senderLapis);
				if (!isFree && !plugin.Method.getAccount(player.getName()).hasEnough(plugin.teleportCost)) {
					msgPlayer(player,ChatColor.RED + "You don't have enough to pay for a teleport.");
					return;
				}
				
				Sign receiverSign = (Sign)receiverLapis.getRelative(BlockFace.DOWN).getState();
			
				if (!plugin.disableTeleportMessage) {
					String message;

					if (!plugin.disableTeleportWait) {
						if (receiverSign.getLine(3).equals("")) {
							message = "Preparing to send you, stand still!";
						} else {
							message = "Preparing to send you to "
								 + ChatColor.YELLOW + receiverSign.getLine(3)
								 + ChatColor.AQUA + ", stand still!";
						}
					} else {
						if (receiverSign.getLine(3).equals("")) {
							message = "You have been teleported!";
						} else {
							message = "You have been teleported to "
								 + ChatColor.YELLOW + receiverSign.getLine(3);
						}
					}
					msgPlayer(player,message);
				}
				if (plugin.disableTeleportWait) {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(player,player.getLocation(),senderLapis,receiverLapis,isFree,plugin.disableTeleportWait));
				} else {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(player,player.getLocation(),senderLapis,receiverLapis,isFree,plugin.disableTeleportWait),plugin.sendWait * 20L);
			   }


			}
		}
		//Creating a telepad link
		else if (event.getItem() != null
		&& event.getItem().getType() == Material.REDSTONE
		&& event.getClickedBlock() != null
		&& isTelepadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))) {
			//Verify permissions
			if (!player.hasPermission("bluetelepads.create")) {			
				msgPlayer(player,"You do not have permission to create a telepad!");
				return;
			}
			if (!player.hasPermission("bluetelepads.createfree")) {
				msgPlayer(player,"You do not have permission to create a free telepad.");
				return;
			}
			
			if (getTelepadLapisReceiver(event.getClickedBlock().getRelative(BlockFace.DOWN)) != null) {
				msgPlayer(player,"Error: This telepad seems to be linked already!");
				msgPlayer(player,ChatColor.YELLOW + "You can reset it by breaking the pressure pad on top of it, then clicking the lapis with redstone.");

				return;
			}

			//Determine the action
			if (!lapisLinks.containsKey(player.getName())) {
				//Initial telepad click
				lapisLinks.put(player.getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());
				msgPlayer(player,"Telepad location stored!");
				return;
			} else {
				//They have a stored location, and right clicked  a telepad lapis, so remove the temp location
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					lapisLinks.remove(player.getName());
					msgPlayer(player,"Telepad location ditched! (right clicked)");
					return;
				} else {
					//Setting up the second link
					Block firstLapis = lapisLinks.get(player.getName()).getBlock();

					if (isTelepadLapis(firstLapis)) {
						Block secondLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);

						if (!TelepadsWithinDistance(firstLapis,secondLapis)) {
							msgPlayer(player,ChatColor.RED + "Error: Telepads are too far apart! (Distance:" + getDistance(firstLapis.getLocation(),event.getClickedBlock().getLocation()) + ",MaxAllowed:" + plugin.maxDistance + ")");

							return;
						}

						//The same telepad?
						if (firstLapis == secondLapis) {
							msgPlayer(player,ChatColor.RED + "Error: You cannot connect a telepad to itself.");
							lapisLinks.remove(player.getName());
							return;
						}

						lapisLinks.remove(player.getName());
						linkTelepadLapisReceivers(firstLapis,event.getClickedBlock().getRelative(BlockFace.DOWN));
						msgPlayer(player,"Telepad location transferred!");
						return;
					}
				}
			}
		}
		//Resetting telepad
		else if (event.getItem() != null
		&& event.getItem().getType() == Material.REDSTONE
		&& event.getClickedBlock() != null
		&& event.getClickedBlock().getTypeId() == plugin.telepadCenterID) {
			Block resetLapis = event.getClickedBlock();
			if (resetLapis.getType() == Material.AIR
			&& (isTelepadLapis(resetLapis))) {//*phew*
				//We checked that it's a sign above
				Sign resetSign = (Sign)resetLapis.getRelative(BlockFace.DOWN).getState();

				resetSign.setLine(1,"");
				resetSign.setLine(2,"");
				resetSign.update();

				msgPlayer(player,"Telepad Reset!");

				return;
			}
		}
	}

	private class BluePadTeleport implements Runnable {
		private final Player player;
		private final Location playerLocation;
		private final Block sender;
		private final Block receiver;
		private boolean isFree;
		private final boolean disableTeleportWait;

		BluePadTeleport(Player player,Location playerLocation,Block senderLapis,Block receiverLapis,boolean isFree,boolean disableTeleportWait) {
			this.player = player;
			this.playerLocation = playerLocation;
			this.sender = senderLapis;
			this.receiver = receiverLapis;
			this.isFree = isFree;
			this.disableTeleportWait = disableTeleportWait;
		}

		public void run() {
			if (getDistance(playerLocation,player.getLocation()) > 1) {
				msgPlayer(player,"You moved, cancelling teleport!");
				return;
			}
			if (!this.disableTeleportWait) {
				msgPlayer(player,"Here goes nothing!");
			}

			Location sendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
			sendTo.setX(sendTo.getX() + 0.5);
			sendTo.setZ(sendTo.getZ() + 0.5);

			sendTo.setPitch(player.getLocation().getPitch());

			Block sign = receiver.getRelative(BlockFace.DOWN);

			if (sign.getType() == Material.SIGN_POST) {
				sendTo.setYaw(sign.getData()*22.5f);
			} else if (sign.getType() == Material.WALL_SIGN) {
				byte signData = sign.getData();

				if (signData == 0x2) {//East
					sendTo.setYaw(180);
					if (plugin.useSlabAsDestination) sendTo.setZ(sendTo.getZ() + 1);
				} else if (signData == 0x3) {//West
					sendTo.setYaw(0);
					if (plugin.useSlabAsDestination) sendTo.setZ(sendTo.getZ() - 1);
				} else if (signData == 0x4) {//North
					sendTo.setYaw(270);
					if (plugin.useSlabAsDestination) sendTo.setX(sendTo.getZ() - 1);
				} else {//South
					sendTo.setYaw(90);
					if (plugin.useSlabAsDestination) sendTo.setX(sendTo.getZ() + 1);
				}
			} else {
				sendTo.setYaw(player.getLocation().getYaw());
			}

			//TODO did making this private class non-static break it?
			if (!isFree && plugin.Method != null) {
				plugin.Method.getAccount(player.getName()).subtract(plugin.teleportCost);
				msgPlayer(player,"You have been charged " + plugin.teleportCost + ".");
			}
			player.teleport(sendTo);

			teleportTimeouts.put(player.getName(),System.currentTimeMillis() + Math.min(plugin.telepadCooldown,1) * 1000);
		}
	}
}