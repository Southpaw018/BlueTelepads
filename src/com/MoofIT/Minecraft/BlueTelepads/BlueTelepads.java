package com.MoofIT.Minecraft.BlueTelepads;

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
import com.nijikokun.register.payment.Method;

/**
 * @author Jim Drey
 *
 */
public class BlueTelepads extends JavaPlugin {
	private final BlueTelepadsPlayerListener playerListener = new BlueTelepadsPlayerListener(this);
	private BlueTelepadsServerListener serverListener;

	public static Logger log;
	public PluginManager pm;
	public PluginDescriptionFile pdfFile;
	private Configuration config;

	public Method Method = null;

	//Config defaults
	public int maxDistance = 0;
	public boolean disableTeleportMessage = false;
	public int telepadCenterID = 22;

	public boolean disableTeleportWait = false;
	public int sendWait = 3;

	public double teleportCost = 0;
	public short telepadSurroundingNormal = 0;
	public short telepadSurroundingFree = 1;

	public void onEnable() {
		log = Logger.getLogger("Minecraft");
		pm = getServer().getPluginManager();
		pdfFile = getDescription();

		loadConfig();
		if (!loadRegister()) return;

		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		//TODO more event registrations here?

		log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
	}

	public void onDisable() {
		log.info("[BlueTelepads] Shutting down.");
		pdfFile = null;
		Method = null;
		pm = null;
	}

	private void loadConfig() {
		config = this.getConfiguration();

		//TODO check configVer, etc
		maxDistance = config.getInt("Core.maxTelepadDistance",maxDistance);
		disableTeleportMessage = config.getBoolean("Core.disableTeleportMessage",disableTeleportMessage);
		telepadCenterID = config.getInt("Core.telepadCenterID",telepadCenterID);

		disableTeleportWait = config.getBoolean("Time.disableTeleportWait",disableTeleportWait);
		sendWait = config.getInt("Time.sendWait", sendWait);
		
		teleportCost = config.getDouble("Economy.teleportCost", teleportCost);
		telepadSurroundingNormal = (short)config.getInt("Economy.telepadSurroundingNormal", telepadSurroundingNormal);
		telepadSurroundingFree = (short)config.getInt("Economy.telepadSurroundingFree", telepadSurroundingFree);
	}

	//returns: true, loaded; false, not loaded OR new
	
	//TODO Javadoc
	private boolean loadRegister() {
		try {
			Class.forName("com.nijikokun.register.payment.Methods");
			serverListener = new BlueTelepadsServerListener(this);
			return true;
		} catch (ClassNotFoundException e) {
			try {
				BlueTelepads.log.info("[BlueTelepads] Register library not found! Downloading...");
				if (!new File("lib").isDirectory())
					if (!new File("lib").mkdir())
						BlueTelepads.log.severe("[BlueTelepads] Error creating lib directory. Please make sure Craftbukkit has permissions to write to the Minecraft directory and there is no file named \"lib\" in that location.");
				URL Register = new URL("https://github.com/iConomy/Register/raw/master/dist/Register.jar");
				ReadableByteChannel rbc = Channels.newChannel(Register.openStream());
				FileOutputStream fos = new FileOutputStream("lib/Register.jar");
				fos.getChannel().transferFrom(rbc, 0, 1 << 24);
				BlueTelepads.log.info("[BlueTelepads] Register library downloaded. Server reboot required to load.");
			} catch (MalformedURLException ex) {
				BlueTelepads.log.warning("[BlueTelepads] Error accessing Register lib URL: " + ex);
			} catch (FileNotFoundException ex) {
				BlueTelepads.log.warning("[BlueTelepads] Error accessing Register lib URL: " + ex);
			} catch (IOException ex) {
				BlueTelepads.log.warning("[BlueTelepads] Error downloading Register lib: " + ex);
			} finally {
				pm.disablePlugin(this);
			}
			return false;
		}
	}
}