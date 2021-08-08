package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.Util;
import biz.donvi.jakesRTP.claimsIntegrations.ClaimsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static biz.donvi.jakesRTP.claimsIntegrations.LrWorldGuard.registerWorldGuardFlag;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class JakesRtpPlugin extends JavaPlugin {


    //<editor-fold desc="======== Static Fields ========">
    static JakesRtpPlugin        plugin;
    static Map<String, Object>   cmdMap;
    static LocationCacheFiller   locFinderRunnable;
    static WorldBorderPluginHook worldBorderPluginHook;
    static ClaimsManager         claimsManager;

    private static       Logger logger;
    private static final String LANG_SETTINGS_FILE_NAME = "language-settings.yml";
    private static final String BLANK_LANG_FILE_NAME    = "translations/lang_%s.yml";
    //</editor-fold>

    //<editor-fold desc="======= NonStatic Fields ========">
    private RandomTeleporter theRandomTeleporter = null;

    private Path toRtpSettings;
    private Path toDistSettings;

    private Economy economy;
    private boolean hasEconomy;

    private boolean locCache = false;
    //</editor-fold>

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null)
            registerWorldGuardFlag(this);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        locCache = true;

        //Set up the reference for some objects
        plugin = this;
        logger = plugin.getLogger();
        cmdMap = new Yaml().load(this.getClassLoader().getResourceAsStream("commandTree.yml"));
        worldBorderPluginHook = new WorldBorderPluginHook(getServer());

        hasEconomy = setupEconomy();
        loadConfigs(); // Loads the default configs if no configs are there
        getCommand("rtp-admin").setExecutor(new CmdRtpAdmin(Util.getImpliedMap(cmdMap, "rtp-admin")));
        loadMessageMap(); // Loads all the messages that get sent by the plugin
        loadRandomTeleporter(); // Loads the random teleporter
        loadLocationCacheFiller(); // Loads the location cache filler

        if (!getConfig().getBoolean("land-claim-support.force-disable-all", false))
            claimsManager = new ClaimsManager(this, getConfig().getConfigurationSection("land-claim-support"));
        new MetricsCustomizer(this, new Metrics(this, 9843));
        infoLog("Loading complete.");
    }

    @Override
    public void onDisable() {
        locCache = false;
        HandlerList.unregisterAll(this);
        theRandomTeleporter = null;
        locFinderRunnable.markAsOver();
        Bukkit.getScheduler().cancelTasks(this);
    }

    public boolean locCache() {
        return locCache;
    }

    /* ================================================== *\
                    Loading methods
    \* ================================================== */


    void reloadCommands() {
        HandlerList.unregisterAll(this);
        getCommand("rtp-admin").setExecutor(new CmdRtpAdmin(Util.getImpliedMap(cmdMap, "rtp-admin")));
    }

    //<editor-fold desc="Loading Methods">
    @SuppressWarnings("ConstantConditions")
    private void loadConfigs() {
        // If there is no config files, save the default ones
        if (!Files.exists(Paths.get(this.getDataFolder().getPath(), "config.yml"))) saveDefaultConfig();
        try {// For the rtpSettings...
            toRtpSettings = Paths.get(this.getDataFolder().getPath(), "rtpSettings");
            if (!Files.exists(toRtpSettings))
                Files.createDirectory(toRtpSettings);
            if (GeneralUtil.isDirEmpty(toRtpSettings))
                Files.copy(
                    getClassLoader().getResourceAsStream("rtpSettings/default-settings.yml"),
                    Paths.get(getDataFolder().getPath(), "rtpSettings", "default-settings.yml"));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not copy default rtpSetting.");
        }
        try {// For the distributions...
            toDistSettings = Paths.get(this.getDataFolder().getPath(), "distributions");
            if (!Files.exists(toDistSettings))
                Files.createDirectory(toDistSettings);
            if (GeneralUtil.isDirEmpty(toDistSettings)) {
                Files.copy(
                    getClassLoader().getResourceAsStream("distributions/default-rectangle.yml"),
                    Paths.get(getDataFolder().getPath(), "distributions", "default-rectangle.yml"));
                Files.copy(
                    getClassLoader().getResourceAsStream("distributions/default-symmetric.yml"),
                    Paths.get(getDataFolder().getPath(), "distributions", "default-symmetric.yml"));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not copy default rtpSetting.");
        }

    }

    private boolean setupEconomy() {
        economy = null;
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;
        economy = rsp.getProvider();
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    public void loadRandomTeleporter() {
        this.reloadConfig();
        try {
            theRandomTeleporter =
                new RandomTeleporter(
                    this.getConfig(),
                    GeneralUtil.getFileConfigFromFile(toRtpSettings.toFile().listFiles()),
                    GeneralUtil.getFileConfigFromFile(toDistSettings.toFile().listFiles()));
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
            infoLog("Setting up the location caching system.");
            Bukkit.getScheduler().runTaskAsynchronously(this, (
                locFinderRunnable = new LocationCacheFiller(
                    this,
                    (long) (getConfig().getDouble("location-cache-filler.recheck-time", 2) * 1000),
                    (long) (getConfig().getDouble("location-cache-filler.between-time", 0.5) * 1000))
            ));
        }
    }

    String lang = "en";

    int customMessageCount = 0;

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
        lang = messageOverrides.get("language");
        if (!lang.equalsIgnoreCase("en")) {
            languageOverrides = new Yaml().load(this.getClassLoader().getResourceAsStream(
                String.format(BLANK_LANG_FILE_NAME, lang)));
            infoLog("Overwriting default messages with translated messages.");
            Messages.addMap(languageOverrides);
        }
        // Load message overrides.
        messageOverrides.remove("language");
        infoLog("Overwriting messages with custom messages.");
        customMessageCount = Messages.addMap(messageOverrides);
    }
    //</editor-fold>

    /* ================================================== *\
                   Getters
    \* ================================================== */

    //<editor-fold desc="Getters">
    public Economy getEconomy() {return economy;}

    public boolean canUseEconomy() {return hasEconomy;}

    public String getCurrentConfigVersion() {return getConfig().getString("config-version");}

    public RandomTeleporter getRandomTeleporter() {return theRandomTeleporter;}
    //</editor-fold>

    /* ================================================== *\
                    Logging Related
    \* ================================================== */

    //<editor-fold desc="Logging Related">
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
        logger.log(level, msg);
    }

    public static void infoLog(String msg) {
        log(Level.INFO, msg);
    }
    //</editor-fold>

}
