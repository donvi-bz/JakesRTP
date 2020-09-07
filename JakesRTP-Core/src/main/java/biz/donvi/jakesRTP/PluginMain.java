package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.Util;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PluginMain extends JavaPlugin {

    public static PluginMain plugin;
    static Logger logger;
    static Map<String, Object> cmdMap;
    static LocationCacheFiller locFinderRunnable;

    private static Map<String, String> messages;
    private RandomTeleporter theRandomTeleporter;
    private String defaultConfigVersion = null;


    @Override
    public void onEnable() {
        //Set up the reference for some objects
        plugin = this;
        logger = plugin.getLogger();
        cmdMap = new Yaml().load(this.getClassLoader().getResourceAsStream("commandTree.yml"));


        try {
            //If there is no config file, save the default one
            if (!Files.exists(Paths.get(this.getDataFolder().getPath(), "config.yml")))
                saveDefaultConfig();
            else if (!getCurrentConfigVersion().equals(getDefaultConfigVersion())
                     && !getConfig().getBoolean("run-old-configs")) {
                logger.log(Level.WARNING, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                logger.log(Level.WARNING, "A new plugin-level config file is available.");
                logger.log(Level.WARNING, "Automatically backing up the old config file, and using the new default one.");
                logger.log(Level.WARNING, "You may want to copy any values from the old config to the new one if you customized it at all.");
                logger.log(Level.WARNING, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
                Files.move(
                        Paths.get(getDataFolder().getPath() + "/config.yml"),
                        Paths.get(getDataFolder().getPath() + "/config-" + getCurrentConfigVersion() + "-old.yml")
                );
                saveDefaultConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //Register commands
        Objects.requireNonNull(getCommand("rtp-admin"))
                .setExecutor(new CmdRtpAdmin(Util.getImpliedMap(cmdMap, "rtp-admin")));
        loadMessageMap(); //DON'T REMOVE THIS LINE, it loads all the messages that get sent by the plugin
        loadRandomTeleporter(); //DON'T REMOVE THIS LINE, THE MAJORITY OF THE FUNCTIONALITY COMES FROM IT
        loadLocationCacheFiller(); //DON'T REMOVE THIS LINE, IT IS REQUIRED FOR LOCATION CACHING TO WORK

        System.out.println("Loading complete.");
    }

    public static void infoLog(String msg) {
        logger.log(Level.INFO, msg);
    }

    public static String getMessage(String key, Object... args) {
        return MessageFormat.format(messages.get(key), args);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        theRandomTeleporter = null;
        defaultConfigVersion = null;
        locFinderRunnable.markAsOver();
        Bukkit.getScheduler().cancelTasks(this);
    }

    @SuppressWarnings("ConstantConditions")
    public void loadRandomTeleporter() {
        this.reloadConfig();
        try {
            theRandomTeleporter = new RandomTeleporter(this.getConfig());
            getCommand("rtp").setExecutor(
                    new CmdRtp(theRandomTeleporter));
            getCommand("forcertp").setExecutor(
                    new CmdForceRtp(theRandomTeleporter, Util.getImpliedMap(cmdMap, "forcertp")));
            getServer().getPluginManager().registerEvents(
                    new RtpOnEvent(theRandomTeleporter), this);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "RTP Command could not be loaded!");
            e.printStackTrace();
        }
    }

    public void loadLocationCacheFiller() {
        //First end the current runnable if it exists
        if (locFinderRunnable != null) locFinderRunnable.markAsOver();
        //Then load up a new runnable
        if (getConfig().getBoolean("location-cache-filler.enabled", true)) {
            System.out.println("Setting up the location caching system.");
            Bukkit.getScheduler().runTaskAsynchronously(this,
                    (locFinderRunnable = new LocationCacheFiller(
                            this,
                            (long) (getConfig().getDouble("location-cache-filler.recheck-time", 2) * 1000),
                            (long) (getConfig().getDouble("location-cache-filler.between-time", 0.5) * 1000))
                    ));
        }
    }

    public String getDefaultConfigVersion() throws Exception {
        if (defaultConfigVersion != null) return defaultConfigVersion;
        String confVersionLine = "config-version: ";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                this.getClass().getClassLoader().getResourceAsStream("config.yml")
        )));
        try {
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                if (s.startsWith(confVersionLine))
                    return defaultConfigVersion = s.substring(confVersionLine.length());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new Exception("Could not find version of the resource config.yml");
    }

    public String getCurrentConfigVersion() {
        return getConfig().getString("config-version");
    }

    public RandomTeleporter getRandomTeleporter() {

        return theRandomTeleporter;
    }


    private static final String LANG_SETTINGS_FILE_NAME = "language-settings.yml";
    private static final String BLANK_LANG_FILE_NAME = "translations/lang_%s.yml";

    @SuppressWarnings("ConstantConditions")
    public void loadMessageMap() {
        // Copy the langSettingsFile if it doesn't already exist.
        if (!Files.exists(Paths.get(getDataFolder().getPath(), LANG_SETTINGS_FILE_NAME)))
            try {
                Files.copy(
                        getClassLoader().getResourceAsStream(LANG_SETTINGS_FILE_NAME),
                        Paths.get(getDataFolder().getPath(), LANG_SETTINGS_FILE_NAME));
            } catch (IOException e) {
                e.printStackTrace();
            }
        // Read message overrides (this also tells us which language to use!)
        Map<String, String> languageOverrides = null;
        Map<String, String> messageOverrides = null;
        try {
            messageOverrides = new Yaml().load(new FileInputStream(new File(getDataFolder(), LANG_SETTINGS_FILE_NAME)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Load default messages
        try {
            infoLog("Setting default messages.");
            Messages.setMap(new Yaml().load(this.getClassLoader().getResourceAsStream(
                    String.format(BLANK_LANG_FILE_NAME, "en"))));
        } catch (JrtpBaseException e) {
            e.printStackTrace();
        }
        // Load all the messages from the language file
        if (!messageOverrides.get("language").equals("en")) {
            languageOverrides = new Yaml().load(this.getClassLoader().getResourceAsStream(
                    String.format(BLANK_LANG_FILE_NAME, messageOverrides.get("language"))));
            infoLog("Overwriting default messages with translated messages.");
            Messages.addMap(languageOverrides);
        }
        // Load message overrides.
        messageOverrides.remove("language");
        infoLog("Overwriting messages with custom messages.");
        Messages.addMap(messageOverrides);
    }
}
