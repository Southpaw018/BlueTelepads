package me.specops.bluetelepads;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;

import org.bukkit.util.config.Configuration;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.payment.Method;
import org.bukkit.plugin.Plugin;

/**
 * @author Jim Drey
 *
 */
public class BlueTelePads extends JavaPlugin {
	private final BlueTelePadsPlayerListener playerListener = new BlueTelePadsPlayerListener(this);
	private BlueTelePadsServerListener serverListener;

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
	public double TELEPORT_COST = 0;

	public void onEnable(){
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
			config.setProperty("teleport_cost",TELEPORT_COST);

			config.save();
		}

		MAX_DISTANCE = config.getInt("max_telepad_distance",MAX_DISTANCE);
		USE_PERMISSIONS = config.getBoolean("use_permissions",USE_PERMISSIONS);
		OP_ONLY = config.getBoolean("op_only",OP_ONLY);
		DISABLE_TELEPORT_WAIT = config.getBoolean("disable_teleport_wait",DISABLE_TELEPORT_WAIT);
		DISABLE_TELEPORT_MESSAGE = config.getBoolean("disable_teleport_message",DISABLE_TELEPORT_MESSAGE);
		TELEPAD_CENTER_ID = config.getInt("telepad_center",TELEPAD_CENTER_ID);
		TELEPAD_SURROUNDING_ID = config.getInt("telepad_surrounding",TELEPAD_SURROUNDING_ID);
		TELEPORT_COST = config.getDouble("teleport_cost", TELEPORT_COST);

		if(USE_PERMISSIONS){
			Plugin perm = pm.getPlugin("Permissions");
			if(perm != null){
				log.info("[BlueTelePads] Permissions integration enabled");
				Permissions = ((Permissions) perm).getHandler();
			}else{
				log.info("[BlueTelePads] Permissions integration could not be enabled!");
			}
		}

		if (!loadRegister()) return;

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

	//returns: true, loaded; false, not loaded OR new
	private boolean loadRegister() {
		try {
			Class.forName("com.nijikokun.register.payment.Methods");
			serverListener = new BlueTelePadsServerListener(this);
			return true;
		} catch (ClassNotFoundException e) {
			try {
				BlueTelePads.log.info("[BlueTelePads] Register library not found! Downloading...");
				if (!new File("lib").isDirectory())
					if (!new File("lib").mkdir())
						BlueTelePads.log.severe("[BlueTelePads] Error creating lib directory. Please make sure Craftbukkit has permissions to write to the Minecraft directory and there is no file named \"lib\" in that location.");
				URL Register = new URL("https://github.com/iConomy/Register/raw/master/dist/Register.jar");
				ReadableByteChannel rbc = Channels.newChannel(Register.openStream());
				FileOutputStream fos = new FileOutputStream("lib/Register.jar");
				fos.getChannel().transferFrom(rbc, 0, 1 << 24);
				BlueTelePads.log.info("[BlueTelePads] Register library downloaded. Server reboot required to load.");
			} catch (MalformedURLException ex) {
				BlueTelePads.log.warning("[BlueTelePads] Error accessing Register lib URL: " + ex);
			} catch (FileNotFoundException ex) {
				BlueTelePads.log.warning("[BlueTelePads] Error accessing Register lib URL: " + ex);
			} catch (IOException ex) {
				BlueTelePads.log.warning("[BlueTelePads] Error downloading Register lib: " + ex);
			} finally {
				pm.disablePlugin(this);
			}
			return false;
		}
	}
}