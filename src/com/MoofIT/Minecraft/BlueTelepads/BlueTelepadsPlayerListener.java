package com.MoofIT.Minecraft.BlueTelepads;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.World;

public class BlueTelepadsPlayerListener implements Listener {
	private final BlueTelepads plugin;
	private static HashMap<String, Location> lapisLinks  = new HashMap<String, Location>();
	private static HashMap<String, Long> teleportTimeouts = new HashMap<String, Long>();
	private static HashSet<Player> teleportingPlayers = new HashSet<Player>();

	public BlueTelepadsPlayerListener(BlueTelepads instance) {
		this.plugin = instance;
	}

	public static void msgPlayer(Player player,String msg) {
		if (!msg.isEmpty()) player.sendMessage(ChatColor.DARK_AQUA + "[BlueTelepads] " + ChatColor.AQUA + msg);
	}

	public boolean isTelepadLapis(Block lapisBlock) {
		if (!isTelepadLapis(lapisBlock, false)) return false;
		if (lapisBlock.getRelative(BlockFace.UP).getType() != Material.STONE_PLATE) return false;
		return true;
	}
	public boolean isTelepadLapis(Block lapisBlock, boolean resetting) {
		if (lapisBlock.getTypeId() != plugin.telepadCenterID) return false;

		//get the setup of the slab to the north to check that all slabs are the same
		short slabType = lapisBlock.getRelative(BlockFace.NORTH).getData();
		int slabID = lapisBlock.getRelative(BlockFace.NORTH).getTypeId();

		if (slabID != 43 && (plugin.allowSingleSlabs == true && slabID != 44)) return false;
		if (slabType != plugin.telepadSurroundingNormal && slabType != plugin.telepadSurroundingFree) {
			if (!plugin.disableEconomy) return false;
		}

		BlockFace[] surroundingChecks = {BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};
		for (BlockFace check : surroundingChecks) {
			if ((lapisBlock.getRelative(check).getTypeId() != slabID) || lapisBlock.getRelative(check).getData() != slabType) return false;
		}

		if (lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.SIGN_POST && lapisBlock.getRelative(BlockFace.DOWN).getType() != Material.WALL_SIGN) return false;

		return true;
	}

	public boolean isTelepadFree(Block lapisBlock) {
		Sign sign = (Sign)lapisBlock.getRelative(BlockFace.DOWN).getState();
		String[] line0 = sign.getLine(0).split(":");
		if (line0.length != 2 || !line0[1].equals("F")) return false;
		if (isTelepadFree(lapisBlock,false)) return true;
		return false; 
	}
	public boolean isTelepadFree(Block lapisBlock, boolean creatingLink) {
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

		slapis1.setLine(0,"BlueTelepads:" + (isTelepadFree(lapis1,true) ? "F" : "P"));
		slapis2.setLine(0,"BlueTelepads:" + (isTelepadFree(lapis2,true) ? "F" : "P"));

		slapis1.setLine(1,slapis2.getWorld().getName());
		slapis2.setLine(1,slapis1.getWorld().getName());

		Location lLapis1 = lapis1.getLocation();
		Location lLapis2 = lapis2.getLocation();

		slapis1.setLine(2,toHex(lLapis2.getBlockX()) + ":" + toHex(lLapis2.getBlockY()) + ":" + toHex(lLapis2.getBlockZ()));
		slapis2.setLine(2,toHex(lLapis1.getBlockX()) + ":" + toHex(lLapis1.getBlockY()) + ":" + toHex(lLapis1.getBlockZ()));

		slapis1.update(true);
		slapis2.update(true);
	}

	private boolean TelepadsWithinDistance(Block block1,Block block2) {
		if (plugin.maxDistance == 0) {
			return true;
		}
		if (block1.getLocation().distance(block2.getLocation()) < plugin.maxDistance) {
			return true;
		}
		return false;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		//Using a telepad, note we verify the timeout here after checking if it's a telepad
		if (event.getAction() == Action.PHYSICAL
		&& event.getClickedBlock() != null
		&& isTelepadLapis(event.getClickedBlock().getRelative(BlockFace.DOWN))
		&& (!teleportTimeouts.containsKey(player.getName()) || teleportTimeouts.get(player.getName()) < System.currentTimeMillis())
		&& !teleportingPlayers.contains(player)) {
			Block senderLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);
			Block receiverLapis = getTelepadLapisReceiver(senderLapis);
			//Verify receiver is a working telepad
			if (receiverLapis != null) {
				//Verify permissions
				if (!player.hasPermission("bluetelepads.use")) {
					msgPlayer(player,plugin.BlueTelepadsMessages.get("Permissions.Use").toString());
					return;
				}

				//Verify distance
				if (!TelepadsWithinDistance(senderLapis,receiverLapis)) {
					msgPlayer(player,ChatColor.RED + plugin.BlueTelepadsMessages.get("Error.Distance").toString() + " (Distance: " + senderLapis.getLocation().distance(receiverLapis.getLocation()) + ", Max Allowed:" + plugin.maxDistance + ")");
					return;
				}
				boolean isFree;
				if (plugin.disableEconomy) {
					isFree = true;
				}
				else {
					if (player.hasPermission("bluetelepads.alwaysfree")) isFree = true;
					else isFree = isTelepadFree(senderLapis);
					if (!isFree && BlueTelepads.econ != null && (BlueTelepads.econ.getBalance(player.getName()) < plugin.teleportCost)) {
						msgPlayer(player,ChatColor.RED + plugin.BlueTelepadsMessages.get("Economy.InsufficientFunds").toString());
						return;
					}
				}
				
				Sign receiverSign = (Sign)receiverLapis.getRelative(BlockFace.DOWN).getState();
			
				if (!plugin.disableTeleportMessage) {
					String message;

					if (!plugin.disableTeleportWait) {
						if (receiverSign.getLine(3).equals("")) {
							message = plugin.BlueTelepadsMessages.get("Core.TeleportWaitNoName").toString() + " " + plugin.BlueTelepadsMessages.get("Core.WaitInstruction").toString();
						} else {
							message = plugin.BlueTelepadsMessages.get("Core.TeleportWaitWithName").toString()
								+ " " + ChatColor.YELLOW + receiverSign.getLine(3).replace("&", "\u00A7")
								+ ChatColor.AQUA + "! " + plugin.BlueTelepadsMessages.get("Core.WaitInstruction").toString();
						}
					} else {
						if (receiverSign.getLine(3).equals("")) {
							message = plugin.BlueTelepadsMessages.get("Core.NoWaitNoName").toString();
						} else {
							message = plugin.BlueTelepadsMessages.get("Core.NoWaitWithName").toString()
								+ " " + ChatColor.YELLOW + receiverSign.getLine(3);
						}
					}
					msgPlayer(player,message);
				}
				teleportingPlayers.add(player);
				if (plugin.disableTeleportWait) {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(player,senderLapis,receiverLapis,isFree,plugin.disableTeleportWait));
				} else {
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadTeleport(player,senderLapis,receiverLapis,isFree,plugin.disableTeleportWait),plugin.sendWait * 20L);
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
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Permission.Create").toString());
				return;
			}
			if (isTelepadFree(event.getClickedBlock(),true) && !player.hasPermission("bluetelepads.createfree")) {
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Permission.CreateFree").toString());
				return;
			}
			
			if (getTelepadLapisReceiver(event.getClickedBlock().getRelative(BlockFace.DOWN)) != null) {
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Error.AlreadyLinked").toString());
				msgPlayer(player,ChatColor.YELLOW + plugin.BlueTelepadsMessages.get("Error.AlreadyLinkedInstruction").toString());
				return;
			}

			//Determine the action
			if (!lapisLinks.containsKey(player.getName())) {
				//Initial telepad click
				lapisLinks.put(player.getName(),event.getClickedBlock().getRelative(BlockFace.DOWN).getLocation());
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Core.LocationStored").toString());
				return;
			} else {
				//They have a stored location, and right clicked  a telepad lapis, so remove the temp location
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					lapisLinks.remove(player.getName());
					msgPlayer(player,plugin.BlueTelepadsMessages.get("Core.ProcessReset").toString());
					return;
				} else {
					//Setting up the second link
					Block firstLapis = lapisLinks.get(player.getName()).getBlock();

					if (isTelepadLapis(firstLapis)) {
						Block secondLapis = event.getClickedBlock().getRelative(BlockFace.DOWN);

						if (!TelepadsWithinDistance(firstLapis,secondLapis)) {
							msgPlayer(player,ChatColor.RED + plugin.BlueTelepadsMessages.get("Error.Distance").toString() + " (Distance: " + firstLapis.getLocation().distance(event.getClickedBlock().getLocation()) + ", Max Allowed:" + plugin.maxDistance + ")");
							return;
						}

						//The same telepad?
						if (firstLapis == secondLapis) {
							msgPlayer(player,ChatColor.RED + plugin.BlueTelepadsMessages.get("Error.Reflexive").toString());
							lapisLinks.remove(player.getName());
							return;
						}

						lapisLinks.remove(player.getName());
						linkTelepadLapisReceivers(firstLapis,event.getClickedBlock().getRelative(BlockFace.DOWN));
						msgPlayer(player,plugin.BlueTelepadsMessages.get("Core.Activated").toString());
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
			if (isTelepadLapis(resetLapis, true)) {
				Sign resetSign = (Sign)resetLapis.getRelative(BlockFace.DOWN).getState();

				resetSign.setLine(0,"");
				resetSign.setLine(1,"");
				resetSign.setLine(2,"");
				resetSign.update();

				msgPlayer(player,plugin.BlueTelepadsMessages.get("Core.Reset").toString());

				return;
			}
		}
	}

	private class BluePadTeleport implements Runnable {
		private final Player player;
		private final Block sender;
		private final Block receiver;
		private boolean isFree;
		private final boolean disableTeleportWait;

		BluePadTeleport(Player player,Block senderLapis,Block receiverLapis,boolean isFree,boolean disableTeleportWait) {
			this.player = player;
			this.sender = senderLapis;
			this.receiver = receiverLapis;
			this.isFree = isFree;
			this.disableTeleportWait = disableTeleportWait;
		}

		public void run() {
			teleportingPlayers.remove(player);
			Location senderPadCenter = sender.getRelative(BlockFace.UP).getLocation();
			senderPadCenter.setX(senderPadCenter.getBlockX() + 0.5);
			senderPadCenter.setZ(senderPadCenter.getBlockZ() + 0.5);

			if (senderPadCenter.distance(player.getLocation()) > 1.1 || senderPadCenter.getBlockY() != player.getLocation().getBlockY()) {
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Error.PlayerMoved").toString());
				return;
			}
			if (!this.disableTeleportWait) {
				msgPlayer(player,plugin.BlueTelepadsMessages.get("Core.Teleport").toString());
			}

			Location sendTo = receiver.getRelative(BlockFace.UP,2).getLocation();
			sendTo.setX(sendTo.getX() + 0.5);
			sendTo.setZ(sendTo.getZ() + 0.5);

			sendTo.setPitch(player.getLocation().getPitch());

			Block sign = receiver.getRelative(BlockFace.DOWN);

			byte signData = sign.getData();
			if (sign.getType() == Material.SIGN_POST) {
				sendTo.setYaw(signData < 0x8 ? signData*22.5f + 180 : signData*22.5f - 180);
				if (plugin.useSlabAsDestination) {
					if (signData < 0x3 || signData == 0xF) {//west
						sendTo.setZ(sendTo.getZ() - 1);
					}
					else if (signData < 0x7) {//north
						sendTo.setX(sendTo.getX() - 1);
					}
					else if (signData < 0xB) {//east
						sendTo.setZ(sendTo.getZ() + 1);						
					}
					else if (signData < 0xF) {//south
						sendTo.setX(sendTo.getX() + 1);						
					}
				}
			} else if (sign.getType() == Material.WALL_SIGN) {
				if (signData == 0x2) {//East
					sendTo.setYaw(0);
					if (plugin.useSlabAsDestination) sendTo.setZ(sendTo.getZ() + 1);
				} else if (signData == 0x3) {//West
					sendTo.setYaw(180);
					if (plugin.useSlabAsDestination) sendTo.setZ(sendTo.getZ() - 1);
				} else if (signData == 0x4) {//North
					sendTo.setYaw(270);
					if (plugin.useSlabAsDestination) sendTo.setX(sendTo.getX() - 1);
				} else {//South
					sendTo.setYaw(90);
					if (plugin.useSlabAsDestination) sendTo.setX(sendTo.getX() + 1);
				}
			} else {
				sendTo.setYaw(player.getLocation().getYaw());
			}

			if (!plugin.disableEconomy) {
				if (!isFree && BlueTelepads.econ != null && plugin.teleportCost > 0) {
					BlueTelepads.econ.withdrawPlayer(player.getName(), plugin.teleportCost);
					msgPlayer(player,ChatColor.GOLD + BlueTelepads.econ.format(plugin.teleportCost) + ChatColor.AQUA + " has been withdrawn from your account.");
				}
			}
			player.teleport(sendTo);
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new BluePadReset(senderPadCenter), 20L);

			teleportTimeouts.put(player.getName(),System.currentTimeMillis() + Math.min(plugin.telepadCooldown,3) * 1000);
		}
	}

	private class BluePadReset implements Runnable {
		private final Location pressurePlateLoc;

		BluePadReset(Location pressurePlateLoc) {
			this.pressurePlateLoc = pressurePlateLoc;
		}

		public void run() {
			Block pressurePlate = pressurePlateLoc.getBlock();
			BlueTelepads.log.info("[BlueTelepads] [DEBUG] " + pressurePlate.getData());
			if (pressurePlate.getData() == 0x1) {
				pressurePlate.setData((byte)0);
			}
			BlueTelepads.log.info("[BlueTelepads] [DEBUG] " + pressurePlate.getData());			
		}
	}
}