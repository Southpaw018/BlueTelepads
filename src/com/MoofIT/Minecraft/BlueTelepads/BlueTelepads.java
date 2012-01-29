package com.MoofIT.Minecraft.BlueTelepads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
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
	private final BlueTelepadsBlockListener blockListener = new BlueTelepadsBlockListener(this);
	private BlueTelepadsServerListener serverListener;

	public static Logger log;
	public PluginManager pm;
	public PluginDescriptionFile pdfFile;
	private FileConfiguration config;

	public Method Method = null;

	//Config defaults
	public int maxDistance = 0;
	public boolean disableTeleportMessage = false;
	public int telepadCenterID = 22;
	public boolean useSlabAsDestination = false;
	public boolean allowSingleSlabs = false;
	public boolean versionCheck = true;

	public boolean disableTeleportWait = false;
	public int sendWait = 3;
	public int telepadCooldown = 5;

	public boolean disableEconomy = false;
	public double teleportCost = 0;
	public byte telepadSurroundingNormal = 0;
	public byte telepadSurroundingFree = 1;

	public TreeMap<String, Object> BlueTelepadsMessages = new TreeMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put("Error.Distance","Error: Telepads are too far apart!");
			put("Error.AlreadyLinked","Error: This telepad seems to be linked already!");
			put("Error.AlreadyLinkedInstruction","You can reset it by breaking the pressure pad on top of it, then tapping the lapis with redstone.");
			put("Error.Reflexive","Error: You cannot connect a telepad to itself.");
			put("Error.PlayerMoved","You're not on the center of the pad! Cancelling teleport.");

			put("Core.TeleportWaitNoName","Preparing to send you!");
			put("Core.TeleportWaitWithName","Preparing to send you to");
			put("Core.WaitInstruction","Stand on the center of the pad.");
			put("Core.NoWaitNoName","You have been teleported!");
			put("Core.NoWaitWithName","You have been teleported to");
			put("Core.LocationStored","Telepad location stored.");
			put("Core.ProcessReset","Link process reset.");
			put("Core.Activated","Telepad activated!");
			put("Core.Teleport","Here goes nothing!");
			put("Core.Reset","Telepad reset.");

			put("Economy.InsufficientFunds","You don't have enough to pay for a teleport.");
			put("Economy.Charged","You have been charged");

			put("Permission.Use","You do not have permission to use telepads.");
			put("Permission.Create","You do not have permission to create a telepad!");
			put("Permission.CreateFree","You do not have permission to create a free telepad.");
		}
	};

	//Config versioning
	private int configVer = 0;
	private final int configCurrent = 2;

	public void onEnable() {
		log = Logger.getLogger("Minecraft");
		pm = getServer().getPluginManager();
		pdfFile = getDescription();

		loadConfig();
		if (!loadRegister()) return;
		if (versionCheck) versionCheck();

		pm.registerEvents(playerListener,this);
		pm.registerEvents(serverListener,this);
		pm.registerEvents(blockListener,this);

		log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
	}

	public void onDisable() {
		log.info("[BlueTelepads] Shutting down.");
		pdfFile = null;
		Method = null;
		pm = null;
	}

	private void loadConfig() {
		this.reloadConfig();
		config = this.getConfig();

		configVer = config.getInt("configVer", configVer);
		if (configVer == 0) {
			try {
				log.info("[BlueTelepads] Configuration error or no config file found. Downloading default config file...");
				if (!new File(getDataFolder().toString()).exists()) {
					new File(getDataFolder().toString()).mkdir();
				}
				URL config = new URL("https://raw.github.com/Southpaw018/BlueTelepads/master/config.yml");
				ReadableByteChannel rbc = Channels.newChannel(config.openStream());
				FileOutputStream fos = new FileOutputStream(this.getDataFolder().getPath() + "/config.yml");
				fos.getChannel().transferFrom(rbc, 0, 1 << 24);
			} catch (MalformedURLException ex) {
				log.warning("[BlueTelepads] Error accessing default config file URL: " + ex);
			} catch (FileNotFoundException ex) {
				log.warning("[BlueTelepads] Error accessing default config file URL: " + ex);
			} catch (IOException ex) {
				log.warning("[BlueTelepads] Error downloading default config file: " + ex);
			}

		}
		else if (configVer < configCurrent) {
			log.warning("[BlueTelepads] Your config file is out of date! Delete your config and reload to see the new options. Proceeding using set options from config file and defaults for new options..." );
		}

		maxDistance = config.getInt("Core.maxTelepadDistance",maxDistance);
		disableTeleportMessage = config.getBoolean("Core.disableTeleportMessage",disableTeleportMessage);
		telepadCenterID = config.getInt("Core.telepadCenterID",telepadCenterID);
		useSlabAsDestination = config.getBoolean("Core.useSlabAsDestination", useSlabAsDestination);
		allowSingleSlabs = config.getBoolean("Core.allowSingleSlabs", allowSingleSlabs);
		versionCheck = config.getBoolean("Core.versionCheck", versionCheck);

		disableTeleportWait = config.getBoolean("Time.disableTeleportWait",disableTeleportWait);
		sendWait = config.getInt("Time.sendWait", sendWait);
		telepadCooldown = config.getInt("Time.telepadCooldown", telepadCooldown);

		disableEconomy = config.getBoolean("Economy.disableEconomy", disableEconomy);
		teleportCost = config.getDouble("Economy.teleportCost", teleportCost);
		telepadSurroundingNormal = (byte)config.getInt("Economy.telepadSurroundingNormal", telepadSurroundingNormal);
		telepadSurroundingFree = (byte)config.getInt("Economy.telepadSurroundingFree", telepadSurroundingFree);

		//Messages
		try {
			BlueTelepadsMessages = (TreeMap<String, Object>)config.getConfigurationSection("BlueTelepadsMessages").getValues(true);
		} catch (NullPointerException e) {
			log.warning("[BlueTelepads] Configuration failure while loading BlueTelepadsMessages. Using defaults.");
		}
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
				URL Register = new URL("http://www.moofit.com/minecraft/Register.jar");
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

	public void versionCheck() {
		String thisVersion = getDescription().getVersion();
		URL url = null;
		try {
			url = new URL("http://www.moofit.com/minecraft/bluetelepads.ver?v=" + thisVersion);
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			String newVersion = "";
			String line;
			while ((line = in.readLine()) != null) {
				newVersion += line;
			}
			in.close();
			if (!newVersion.equals(thisVersion)) {
				log.warning("[BlueTelepads] BlueTelepads is out of date! This version: " + thisVersion + "; latest version: " + newVersion + ".");
			}
			else {
				log.info("[BlueTelepads] BlueTelepads is up to date at version " + thisVersion + ".");
			}
		}
		catch (MalformedURLException ex) {
			log.warning("[BlueTelepads] Error accessing update URL.");
		}
		catch (IOException ex) {
			log.warning("[BlueTelepads] Error checking for update.");
		}
	}

	//TODO combine config and register lib file writing code into a function
}