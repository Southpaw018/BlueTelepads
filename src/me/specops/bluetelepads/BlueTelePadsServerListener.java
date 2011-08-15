package me.specops.bluetelepads;
import com.nijikokun.register.payment.Methods;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

//Credit for the below: Nijikokun @ https://github.com/iConomy/Register/blob/master/src/com/nijikokun/register/example/listeners/server.java :)
public class BlueTelePadsServerListener extends ServerListener {
	private BlueTelePads plugin;
	private Methods Methods = null;

	public BlueTelePadsServerListener(BlueTelePads plugin) {
		this.plugin = plugin;
		this.Methods = new Methods();
	}

	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		// Check to see if the plugin thats being disabled is the one we are using
		if (Methods != null && Methods.hasMethod()) {
			Boolean check = this.Methods.checkDisabled(event.getPlugin());

			if(check) {
				this.plugin.Method = null;
				System.out.println("[" + plugin.pdfFile.getName() + "] Payment method was disabled. No longer accepting payments.");
			}
		}
	}

	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		// Check to see if we need a payment method
		if (!this.Methods.hasMethod()) {
			if(this.Methods.setMethod(event.getPlugin())) {
				this.plugin.Method = this.Methods.getMethod();
				System.out.println("[" + plugin.pdfFile.getName() + "] Payment method found (" + plugin.Method.getName() + " version: " + plugin.Method.getVersion() + ")");
			}
		}
	}
}