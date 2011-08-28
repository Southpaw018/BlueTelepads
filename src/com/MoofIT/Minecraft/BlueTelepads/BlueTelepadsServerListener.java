package com.MoofIT.Minecraft.BlueTelepads;

import com.nijikokun.register.payment.Methods;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

//Credit for the below: Nijikokun @ https://github.com/iConomy/Register/blob/master/src/com/nijikokun/register/example/listeners/server.java :)
public class BlueTelepadsServerListener extends ServerListener {
	private BlueTelepads plugin;
	private Methods Methods;

	public BlueTelepadsServerListener(BlueTelepads plugin) {
		this.plugin = plugin;
		Methods = new Methods();
	}

	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		if (Methods != null && Methods.hasMethod()) {
			Boolean check = Methods.checkDisabled(event.getPlugin());

			if(check) {
				plugin.Method = null;
				BlueTelepads.log.info("[BlueTelepads] Payment method disabled. No longer accepting payments.");
			}
		}
	}

	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		// Check to see if we need a payment method
		if (!Methods.hasMethod()) {
			if(Methods.setMethod(event.getPlugin())) {
				plugin.Method = Methods.getMethod();
				BlueTelepads.log.info("[BlueTelepads] Payment method found (" + plugin.Method.getName() + " version: " + plugin.Method.getVersion() + ")");
			}
		}
	}
}