package com.MoofIT.Minecraft.BlueTelepads;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

public class BlueTelepadsBlockListener extends BlockListener {
	//private BlueTelepads plugin;

	public BlueTelepadsBlockListener(BlueTelepads instance) {
		//this.plugin = instance;
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();

		if (block.getType() != Material.WALL_SIGN) return;

		Sign sign = (Sign)block.getState();
		String[] line0 = sign.getLine(0).split(":");

		if (line0.length != 2 || !line0[0].equals("BlueTelepads") || !sign.getLine(1).equals(sign.getBlock().getWorld().getName())) return;
		if (!event.getPlayer().hasPermission("bluetelepads.destroy")) {
			event.setCancelled(true);
			sign.update();
			return;
		}
	}
}
