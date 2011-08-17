package me.specops.bluetelepads;

import java.util.logging.Logger;

import me.specops.bluetelepads.register.payment.Method;

import org.bukkit.util.config.Configuration;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import org.bukkit.plugin.Plugin;

public class BlueTelePads extends JavaPlugin {
	private final BlueTelePadsPlayerListener playerListener = new BlueTelePadsPlayerListener(this);
	private final BlueTelePadsServerListener serverListener = new BlueTelePadsServerListener(this);

	public static Logger log;
	public static PermissionHandler Permissions;
	public PluginManager pm;
	public PluginDescriptionFile pdfFile;

	public Method Method = null;

	public int MAX_DISTANCE = 0;
	public boolean USE_PERMISSIONS = false;
	public boolean OP_ONLY = false;
	public boolean DISABLE_TELEPORT_WAIT = false;
	public boolean DISABLE_TELEPORT_MESSAGE = false;
	public int TELEPAD_CENTER_ID = 22;
	public int TELEPAD_SURROUNDING_ID = 43;
	public long TELEPORT_COST = 0;


	public void onEnable(){
		System.out.println("[BlueTelePads] [Debug] Enabling...");
		log = Logger.getLogger("Minecraft");
		Configuration config = this.getConfiguration();
		pm = getServer().getPluginManager();
		pdfFile = getDescription();

		//set default values if necessary
		if(config.getInt("max_telepad_distance",-1) == -1){
			log.info("[BlueTelepads] Creating default config file...");

			config.setProperty("max_telepad_distance",MAX_DISTANCE);
			config.setProperty("use_permissions",USE_PERMISSIONS);
			config.setProperty("op_only",OP_ONLY);
			config.setProperty("disable_teleport_wait",DISABLE_TELEPORT_WAIT);
			config.setProperty("disable_teleport_message", DISABLE_TELEPORT_MESSAGE);
			config.setProperty("telepad_center",TELEPAD_CENTER_ID);
			config.setProperty("telepad_surrounding",TELEPAD_SURROUNDING_ID);

			config.save();
		}

		MAX_DISTANCE = config.getInt("max_telepad_distance",MAX_DISTANCE);
		USE_PERMISSIONS = config.getBoolean("use_permissions",USE_PERMISSIONS);
		OP_ONLY = config.getBoolean("op_only",OP_ONLY);
		DISABLE_TELEPORT_WAIT = config.getBoolean("disable_teleport_wait",DISABLE_TELEPORT_WAIT);
		DISABLE_TELEPORT_MESSAGE = config.getBoolean("disable_teleport_message",DISABLE_TELEPORT_MESSAGE);
		TELEPAD_CENTER_ID = config.getInt("telepad_center",TELEPAD_CENTER_ID);
		TELEPAD_SURROUNDING_ID = config.getInt("telepad_surrounding",TELEPAD_SURROUNDING_ID);

		if(USE_PERMISSIONS){
			Plugin perm = this.getServer().getPluginManager().getPlugin("Permissions");
			if(perm != null){
				log.info("[BlueTelePads] Permissions integration enabled");
				Permissions = ((Permissions) perm).getHandler();
			}else{
				log.info("[BlueTelePads] Permissions integration could not be enabled!");
			}
		}

		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		//TODO more event registrations here

		log.info("["+pdfFile.getName() + "] version " + pdfFile.getVersion() + " ENABLED" );
	}

	public void onDisable(){
		log.info("[BlueTelePads] Disabled");
		pdfFile = null;
		Method = null;
		pm = null;
	}
}