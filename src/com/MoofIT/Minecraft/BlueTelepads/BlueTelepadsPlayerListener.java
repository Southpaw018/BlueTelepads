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
		BlockFace[] surroundingChecks = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
		for (BlockFace check : surroundingChecks) {
			if (lapisBlock.getRelative(check).getTypeId() != 43
			  && (lapisBlock.getRelative(check).getData() != plugin.telepadSurroundingNormal
			    || lapisBlock.getRelative(check).getData() != plugin.telepadSurroundingFree)) return false;
		}
		if (lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.SIGN_POST && lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.WALL_SIGN) return false;
		if (lapisBlock.getRelative(BlockFace.UP).getType() != Material.STONE_PLATE) return false;
		return true;
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
		//Using a telepad, note we verify the timeout here after checking if it's a telepad
		if (event.getAction() == Action.PHYSICAL
		&& event.getClickedBlock() != null
		&& isTelepadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))
		&& (!teleportTimeouts.containsKey(event.getPlayer().getName()) || teleportTimeouts.get(event.getPlayer().getName()) < System.currentTimeMillis())) {
			Block senderLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);
			Block receiverLapis = getTelepadLapisReceiver(senderLapis);

			//Verify receiver is a working telepad
			if (receiverLapis != null) {
				//Verify permissions
				if (!event.getPlayer().hasPermission("bluetelepads.use")) {
					msgPlayer(event.getPlayer(),"You do not have permission to use telepads.");
					return;
				}

				//Verify distance
				if (!TelepadsWithinDistance(senderLapis,receiverLapis)) {
					msgPlayer(event.getPlayer(),ChatColor.RED + "Error: Telepads are too far apart! (Distance:" + getDistance(senderLapis.getLocation(),receiverLapis.getLocation()) + ",MaxAllowed:" + plugin.maxDistance + ")");
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
					msgPlayer(event.getPlayer(),message);
				}
				if (plugin.disableTeleportWait) {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(event.getPlayer(),event.getPlayer().getLocation(),senderLapis,receiverLapis,plugin.disableTeleportWait));
				} else {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(event.getPlayer(),event.getPlayer().getLocation(),senderLapis,receiverLapis,plugin.disableTeleportWait),plugin.sendWait * 20L);
			   }


			}
		}
		//Creating a telepad link
		else if (event.getItem() != null
		&& event.getItem().getType() == Material.REDSTONE
		&& event.getClickedBlock() != null
		&& isTelepadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))) {
			//Verify permissions
			if (!event.getPlayer().hasPermission("bluetelepads.create")) { //TODO check for createfree			
				msgPlayer(event.getPlayer(),"You do not have permission to create a telepad!");
				return;
			}

			if (getTelepadLapisReceiver(event.getClickedBlock().getRelative(BlockFace.DOWN)) != null) {
				msgPlayer(event.getPlayer(),"Error: This telepad seems to be linked already!");
				msgPlayer(event.getPlayer(),ChatColor.YELLOW + "You can reset it by breaking the pressure pad on top of it, then clicking the lapis with redstone.");

				return;
			}

			//Determine the action
			if (!lapisLinks.containsKey(event.getPlayer().getName())) {
				//Initial telepad click
				lapisLinks.put(event.getPlayer().getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());
				msgPlayer(event.getPlayer(),"Telepad location stored!");
				return;
			} else {
				//They have a stored location, and right clicked  a telepad lapis, so remove the temp location
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					lapisLinks.remove(event.getPlayer().getName());
					msgPlayer(event.getPlayer(),"Telepad location ditched! (right clicked)");
					return;
				} else {
					//Setting up the second link
					Block firstLapis = lapisLinks.get(event.getPlayer().getName()).getBlock();

					if (isTelepadLapis(firstLapis)) {
						Block secondLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);

						if (!TelepadsWithinDistance(firstLapis,secondLapis)) {
							msgPlayer(event.getPlayer(),ChatColor.RED + "Error: Telepads are too far apart! (Distance:" + getDistance(firstLapis.getLocation(),event.getClickedBlock().getLocation()) + ",MaxAllowed:" + plugin.maxDistance + ")");

							return;
						}

						//The same telepad?
						if (firstLapis == secondLapis) {
							msgPlayer(event.getPlayer(),ChatColor.RED + "Error: You cannot connect a telepad to itself.");
							lapisLinks.remove(event.getPlayer().getName());
							return;
						}

						lapisLinks.remove(event.getPlayer().getName());
						linkTelepadLapisReceivers(firstLapis,event.getClickedBlock().getRelative(BlockFace.DOWN));
						msgPlayer(event.getPlayer(),"Telepad location transferred!");
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

				msgPlayer(event.getPlayer(),"Telepad Reset!");

				return;
			}
		}
	}

	private class BluePadTeleport implements Runnable {
		private final Player player;
		private final Location player_location;
		private final Block receiver;
		private final Block sender;
		private final boolean disable_teleport_wait;

		BluePadTeleport(Player player,Location player_location,Block senderLapis,Block receiverLapis,boolean disable_teleport_wait) {
			this.player = player;
			this.player_location = player_location;
			this.sender = senderLapis;
			this.receiver = receiverLapis;
			this.disable_teleport_wait = disable_teleport_wait;
		}

		public void run() {
			if (getDistance(player_location,player.getLocation()) > 1) {
				msgPlayer(player,"You moved, cancelling teleport!");
				return;
			}
			if (!plugin.Method.getAccount(player.getName()).hasEnough(plugin.teleportCost)) {
				msgPlayer(player,"You don't have enough to pay for a teleport.");
				return;
			}

			if (!this.disable_teleport_wait) {
				msgPlayer(player,"Here goes nothing!");
			}

			Location lSendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
			lSendTo.setX(lSendTo.getX() + 0.5);
			lSendTo.setZ(lSendTo.getZ() + 0.5);

			lSendTo.setPitch(player.getLocation().getPitch());

			Block sign = receiver.getRelative(BlockFace.DOWN);

			if (sign.getType() == Material.SIGN_POST) {
				lSendTo.setYaw(sign.getData()*22.5f);
			} else if (sign.getType() == Material.WALL_SIGN) {
				byte signData = sign.getData();

				if (signData == 0x2) {//East
					lSendTo.setYaw(180);
					if (plugin.useSlabAsDestination) lSendTo.setZ(lSendTo.getZ() + 1);
				} else if (signData == 0x3) {//West
					lSendTo.setYaw(0);
					if (plugin.useSlabAsDestination) lSendTo.setZ(lSendTo.getZ() - 1);
				} else if (signData == 0x4) {//North
					lSendTo.setYaw(270);
					if (plugin.useSlabAsDestination) lSendTo.setX(lSendTo.getZ() - 1);
				} else {//South
					lSendTo.setYaw(90);
					if (plugin.useSlabAsDestination) lSendTo.setX(lSendTo.getZ() + 1);
				}
			} else {
				lSendTo.setYaw(player.getLocation().getYaw());
			}

			//TODO did making this private class non-static break it?
			if (plugin.Method != null) {
				plugin.Method.getAccount(player.getName()).subtract(plugin.teleportCost);
				msgPlayer(player,"You have been charged " + plugin.teleportCost + ".");
			}
			player.teleport(lSendTo);

			teleportTimeouts.put(player.getName(),System.currentTimeMillis() + Math.min(plugin.telepadCooldown,1) * 1000);
		}
	}
}