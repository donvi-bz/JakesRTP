package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class PluginMain extends JavaPlugin {

    /* ======== Static Fields ======== */

    static PluginMain          plugin;
    static Map<String, Object> cmdMap;
    static LocationCacheFiller locFinderRunnable;

    private static       Logger logger;
    private static final String LANG_SETTINGS_FILE_NAME = "language-settings.yml";
    private static final String BLANK_LANG_FILE_NAME    = "translations/lang_%s.yml";

    /* ======== NonStatic Fields ======== */

    private RandomTeleporter theRandomTeleporter  = null;
    private String           defaultConfigVersion = null;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        //Set up the reference for some objects
        plugin = this;
        logger = plugin.getLogger();
        cmdMap = new Yaml().load(this.getClassLoader().getResourceAsStream("commandTree.yml"));

        loadConfig(); // Loads the
        getCommand("rtp-admin").setExecutor(new CmdRtpAdmin(Util.getImpliedMap(cmdMap, "rtp-admin")));
        loadMessageMap(); // Loads all the messages that get sent by the plugin
        loadRandomTeleporter(); // Loads the random teleporter
        loadLocationCacheFiller(); // Loads the location cache filler

        infoLog("Loading complete.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        theRandomTeleporter = null;
        defaultConfigVersion = null;
        locFinderRunnable.markAsOver();
        Bukkit.getScheduler().cancelTasks(this);
    }

    /* ================================================== *\
                    Loading methods
    \* ================================================== */

    private void loadConfig(){
        try {
            //If there is no config file, save the default one
            if (!Files.exists(Paths.get(this.getDataFolder().getPath(), "config.yml")))
                saveDefaultConfig();
            else if (!getCurrentConfigVersion().equals(getDefaultConfigVersion()) &&
                     !getConfig().getBoolean("run-old-configs")) {
                for (String msg : new String[]{
                    "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
                    "A new plugin-level config file is available.",
                    "Automatically backing up the old config file, and using the new default one.",
                    "You may want to copy any values from the old config to the new one if you customized it at all.",
                    "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"})
                    log(Level.WARNING, msg);
                Files.move(
                    Paths.get(getDataFolder().getPath() + "/config.yml"),
                    Paths.get(getDataFolder().getPath() + "/config-" + getCurrentConfigVersion() + "-old.yml")
                );
                saveDefaultConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void loadRandomTeleporter() {
        this.reloadConfig();
        try {
            theRandomTeleporter = new RandomTeleporter(
                this.getConfig(),
                YamlConfiguration.loadConfiguration(
                    new InputStreamReader(
                        this.getClassLoader().getResourceAsStream("distributions/distributions.yml")))); //TODO make read from file
            getCommand("rtpSettings").setExecutor(
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
            infoLog("Setting up the location caching system.");
            Bukkit.getScheduler().runTaskAsynchronously(this, (
                locFinderRunnable = new LocationCacheFiller(
                    this,
                    (long) (getConfig().getDouble("location-cache-filler.recheck-time", 2) * 1000),
                    (long) (getConfig().getDouble("location-cache-filler.between-time", 0.5) * 1000))
            ));
        }
    }

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
        else
            try {
                ArrayList<String> langSettingsHeader = new ArrayList<>();

                // All file related variables
                InputStream defaultLangSettings = getClassLoader().getResourceAsStream(LANG_SETTINGS_FILE_NAME);
                File currentLangSettings = new File(getDataFolder().getPath(), LANG_SETTINGS_FILE_NAME);

                // Add the default headers to the arrayList
                try (BufferedReader defaultIn = new BufferedReader(new InputStreamReader(defaultLangSettings, UTF_8))) {
                    String line;
                    while ((line = defaultIn.readLine()) != null && line.startsWith("#"))
                        langSettingsHeader.add(line);
                }

                // Add everything after the headers from the current langSettings file to the arrayList
                try (
                    BufferedReader actualIn = new BufferedReader(new InputStreamReader(
                        new FileInputStream(currentLangSettings), UTF_8))
                ) {
                    String line;
                    boolean inHeader = true;
                    while ((line = actualIn.readLine()) != null) {
                        if (inHeader && !line.startsWith("#")) inHeader = false;
                        if (!inHeader) langSettingsHeader.add(line);
                    }
                }

                // Write everything to the file.
                try (
                    BufferedWriter actualOut = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(currentLangSettings), UTF_8))
                ) {
                    for (String line : langSettingsHeader)
                        actualOut.write(line + '\n');
                }

            } catch (IOException ex) {
                for (Throwable subEx : ex.getSuppressed()) subEx.printStackTrace();
                ex.printStackTrace();
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
        infoLog("Setting default messages.");
        Messages.setMap(new Yaml().load(this.getClassLoader().getResourceAsStream(
            String.format(BLANK_LANG_FILE_NAME, "en"))));
        // Load & add all the messages from the language file
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

    /* ================================================== *\
                   Getters
    \* ================================================== */

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

    public String getCurrentConfigVersion() { return getConfig().getString("config-version"); }

    public RandomTeleporter getRandomTeleporter() { return theRandomTeleporter; }

    /* ================================================== *\
                    Logging Related
    \* ================================================== */

    private static final Queue<LogMsg> msgLog = new ArrayDeque<>();

    private static final class LogMsg {
        final Level  left;
        final String right;

        LogMsg(Level level, String msg) {
            left = level;
            right = msg;
        }
    }

    public static void log(Level level, String msg) {
        msgLog.add(new LogMsg(level, msg));
        logger.log(Level.INFO, msg);
    }

    public static void infoLog(String msg) {
        log(Level.INFO, msg);
    }

}
