package biz.donvi.jakesRTP;

import biz.donvi.evenDistribution.RandomCords;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.Objects;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

public class RandomTeleporter implements CommandExecutor, Listener {

    final static String explicitPermPrefix = "jakesrtp.use.";

    private final ArrayList<RtpSettings> rtpSettings;

    public final boolean firstJoinRtp;
    public final RtpSettings firstJoinSettings;
    public final World firstJoinWorld;

    public final boolean onDeathRtp;
    public final boolean onDeathRespectBeds;
    public final boolean onDeathRequirePermission;
    public final RtpSettings onDeathSettings;
    public final World onDeathWorld;


    public ArrayList<RtpSettings> getRtpSettings() {
        return rtpSettings;
    }


    /**
     * Creating an instance of the RandomTeleporter object is required to be able to use the command.
     * On creation, all relevant parts of the config are loaded into memory.
     *
     * @param config The configurationSection that holds the relevant data for RTPing
     * @throws Exception A generic exception for any issue had when creating the object.
     *                   I have NOT made my own exceptions, but instead have written different messages.
     */
    public RandomTeleporter(ConfigurationSection config) throws Exception {
        // Modular settings:
        rtpSettings = new ArrayList<>();
        for (String key : config.getKeys(false))
            if (key.startsWith("random-teleport-settings"))
                try {
                    ConfigurationSection configSection = config.getConfigurationSection(key);
                    String configName = key.substring("random-teleport-settings".length() + 1);
                    if (configSection != null && configSection.getBoolean("enabled"))
                        rtpSettings.add(new RtpSettings(
                                configSection,
                                configName));
                    else infoLog("Not loading config " + configName + " since it is marked disabled.");
                } catch (NullPointerException e) {
                    PluginMain.infoLog(
                            "Whoops! Something in the config wasn't right, " +
                            rtpSettings.size() + " configs have been loaded thus far.");
                }
        // Static settings:
        if (firstJoinRtp = config.getBoolean("rtp-on-first-join.enabled")) {
            firstJoinSettings = getRtpSettingsByName(config.getString("rtp-on-first-join.settings"));
            World world = PluginMain.plugin.getServer().getWorld(
                    Objects.requireNonNull(config.getString("rtp-on-first-join.world")));
            if (firstJoinSettings.configWorlds.contains(world))
                firstJoinWorld = world;
            else throw new Exception("The RTP first join world is not an enabled world in the config's settings!");
        } else {
            firstJoinSettings = null;
            firstJoinWorld = null;
        }

        if (onDeathRtp = config.getBoolean("rtp-on-death.enabled")) {
            onDeathRespectBeds = config.getBoolean("rtp-on-death.respect-beds");
            onDeathSettings = getRtpSettingsByName(config.getString("rtp-on-death.settings"));
            onDeathRequirePermission = config.getBoolean("rtp-on-death.require-permission");
            World world = PluginMain.plugin.getServer().getWorld(
                    Objects.requireNonNull(config.getString("rtp-on-death.world")));
            if (onDeathSettings.configWorlds.contains(world))
                onDeathWorld = world;
            else throw new Exception("The RTP first join world is not an enabled world in the config's settings!");
        } else {
            onDeathRespectBeds = false;
            onDeathRequirePermission = false;
            onDeathSettings = null;
            onDeathWorld = null;
        }
    }


    /**
     * Gets the {@code RtpSettings} that are being used by the given world. If more then one {@code RtpSettings}
     * objects are valid, the one with the highest priority will be returned.
     *
     * @param world World to get RTP settings for
     * @return The RtpSettings of that world
     * @throws NotPermittedException If the world does not exist.
     */
    public RtpSettings getRtpSettingsByWorld(World world) throws NotPermittedException {
        RtpSettings finSettings = null;
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.configWorlds)
                if (world.equals(settingWorld) && (
                        finSettings == null
                        || finSettings.priority < settings.priority)
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new NotPermittedException("RTP is not enabled in this world.");
    }


    /**
     * Gets the RtpSettings object that has the given name (as defined in the config).
     *
     * @param name The name of the settings
     * @return The Rtp settings object with the given name
     * @throws Exception If no settings have the given name
     */
    public RtpSettings getRtpSettingsByName(String name) throws Exception {
        for (RtpSettings settings : rtpSettings)
            if (settings.name.equals(name))
                return settings;
        throw new Exception("No RTP settings found with name " + name);
    }

    /**
     * Gets the RtpSettings that a specific player in a specific world should be using. This is intended to be
     * used for players running the rtp command, as it follows all rules that players are held to when rtp-ing.
     *
     * @param player The player whose information will be used to determine the relevant rtp settings
     * @return The RtpSettings for the player to use, normally for when they run the {@code /rtp} command.
     * @throws NotPermittedException If no settings can be used.
     */
    public RtpSettings getRtpSettingsByWorldForPlayer(Player player) throws NotPermittedException {
        RtpSettings finSettings = null;
        World playerWorld = player.getWorld();
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.configWorlds)
                //First, the world must be in the settings to become a candidate
                if (playerWorld.equals(settingWorld) &&
                    //Then we check if the settings are usable from the command
                    settings.commandEnabled &&
                    //Then we check the priority
                    (finSettings == null || finSettings.priority < settings.priority) &&
                    //Then we check if we require explicit perms
                    (!settings.requireExplicitPermission || player.hasPermission(explicitPermPrefix + settings.name))
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new NotPermittedException("RTP is not enabled in this world.");
    }

    /**
     * This method acts as a bridge between this Minecraft specific class and my evenDistribution package
     * by calling the appropriate method from the package, and forwarding the relevant configuration
     * settings that have been saved in memory.
     *
     * @param rtpSettings The Rtp settings to use to get the random points
     * @return A random X and Z coordinate pair.
     * @throws Exception if a shape is not properly defined,
     *                   though realistic error checking beforehand should prevent this issue
     */
    private int[] getRtpXZ(RtpSettings rtpSettings) throws Exception {
        switch (rtpSettings.rtpRegionShape) {
            case SQUARE:
                if (rtpSettings.gaussianShrink == 0) return RandomCords.getRandXySquare(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius);
                else return RandomCords.getRandXySquare(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius,
                        rtpSettings.gaussianShrink,
                        rtpSettings.gaussianCenter);
            case CIRCLE:
                return RandomCords.getRandXyCircle(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius,
                        rtpSettings.gaussianShrink,
                        rtpSettings.gaussianCenter);
            case RECTANGLE:
                //return getRtpXzRectangle(); //This will get un-commented once I write a method for rectangles
            default:
                throw new Exception("RTP Region shape not properly defined.");
        }
    }

    /**
     * Creates the potential RTP location. If this location happens to be safe, is will be the exact location that
     * the player gets teleported to (though that is unlikely as the {@code y} is {@code 255} by default). <p>
     * This method differs from {@code getRtpXZ()} because it includes the offset and returns a {@code Location}
     * whereas {@code getRtpZX()} only gets the initial {@code x} and {@code z}, and returns a coordinate pair.
     *
     * @param callFromLoc A location representing where the call originated from. This is used to get either the world
     *                    spawn, or player location for the position offset
     * @param rtpSettings The relevant settings for RTP
     * @return The first location to check the safety of, which may end up being the final teleport location
     * @throws Exception Unlikely, but still possible.
     */
    @SuppressWarnings("ConstantConditions")
    private Location getPotentialRtpLocation(Location callFromLoc, RtpSettings rtpSettings) throws Exception {
        int[] xz = getRtpXZ(rtpSettings);
        int[] xzOffset;
        switch (rtpSettings.centerLocation) {
            case PLAYER_LOCATION:
                xzOffset = new int[]{
                        (int) callFromLoc.getX(),
                        (int) callFromLoc.getZ()};
                break;
            case WORLD_SPAWN:
                xzOffset = new int[]{
                        (int) callFromLoc.getWorld().getSpawnLocation().getX(),
                        (int) callFromLoc.getWorld().getSpawnLocation().getZ()};
                break;
            case PRESET_VALUE:
            default:
                xzOffset = new int[]{
                        rtpSettings.centerX,
                        rtpSettings.centerZ};
        }

        return new Location(
                callFromLoc.getWorld(),
                xz[0] + xzOffset[0],
                255,
                xz[1] + xzOffset[1]
        );
    }

    /**
     * Keeps getting potential teleport locations until one has been found.
     * A fail-safe is included to throw an exception if too many unsuccessful attempts have been made.
     *
     * @param player
     * @param force
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception
     */
    public Location getRtpLocation(Player player, boolean force) throws Exception {
        if (force) return getRtpLocation(
                getRtpSettingsByWorld(player.getWorld()),
                player.getLocation());
        else return getRtpLocation(
                getRtpSettingsByWorldForPlayer(player),
                player.getLocation());
    }

    /**
     * Keeps getting potential teleport locations until one has been found.
     * A fail-safe is included to throw an exception if too many unsuccessful attempts have been made.
     * This method can bypass explicit permission checks.
     *
     * @param rtpSettings The specific RtpSettings to get the location with.
     * @param callFromLoc The location that the call originated from. Used to find the world spawn,
     *                    or player's current location.
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception Only two points of this code are expected to be able to throw an exception:
     *                   getWorldRtpSettings() will throw an exception if the world is not RTP enabled.
     *                   getRtpXZ() will throw an exception if the rtp shape is not defined.
     */
    public Location getRtpLocation(RtpSettings rtpSettings, Location callFromLoc) throws Exception {
        long timeStart = System.currentTimeMillis();
        infoLog("Player used RTP. Finding location...");

        Location potentialRtpLocation;
        int randAttemptCount = 0;
        do {
            potentialRtpLocation = getPotentialRtpLocation(callFromLoc, rtpSettings);
            if (randAttemptCount++ > rtpSettings.maxAttempts)
                throw new NotPermittedException("Too many failed attempts.");
        } while (
                !new SafeLocationFinder(
                        potentialRtpLocation,
                        rtpSettings.checkRadiusXZ,
                        rtpSettings.checkRadiusVert,
                        rtpSettings.lowBound
                ).tryAndMakeSafe()
        );
        infoLog("Location chosen:" +
                " (" + potentialRtpLocation.getX() +
                ", " + potentialRtpLocation.getY() +
                ", " + potentialRtpLocation.getZ() +
                ") in world " + Objects.requireNonNull(potentialRtpLocation.getWorld()).getName());

        long timeElapsed = System.currentTimeMillis() - timeStart;
        infoLog("Location found in " + timeElapsed + " milliseconds after " + randAttemptCount + " attempt(s).");
        if (timeElapsed > 1000)
            infoLog("Note: long search times are mostly caused by the server generating and loading new areas.");
        return potentialRtpLocation;

    }

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - *|
            |* - - - - If a player tries to RTP themselves - - - - *|
            |* - - - - - - - - - - - - - - - - - - - - - - - - - - */
            if (args.length == 0 && sender instanceof Player) {
                Player player = (Player) sender;
                long callTime = System.currentTimeMillis();

                CoolDownTracker coolDownTracker = getRtpSettingsByWorldForPlayer(player).coolDown;

                if (player.hasPermission("jakesRtp.noCooldown") || coolDownTracker.check(player.getName())) {
                    player.teleport(getRtpLocation(player, false));
                    coolDownTracker.log(player.getName(), callTime);
                } else {
                    player.sendMessage("Need to wait for cooldown: " + coolDownTracker.timeLeftWords(player.getName()));
                }
            }
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - *|
            |* - - If a player tries to RTP someone else - - - - - *|
            |* - - - - - - - - - - - - - - - - - - - - - - - - - - */
            else if (args.length == 1 &&
                     sender.hasPermission("jakesRtp.others")
            ) {
                Player playerToTp = sender.getServer().getPlayerExact(args[0]);
                if (playerToTp == null)
                    sender.sendMessage("Could not find player " + args[0]);
                else playerToTp.teleport(
                        getRtpLocation(playerToTp, true));
            }


        } catch (NotPermittedException npe) {
            sender.sendMessage("Could not RTP for reason: " + npe.getMessage());
        } catch (Exception e) {
            sender.sendMessage("Error. Could not RTP for reason: " + e.getMessage());
            sender.sendMessage("Please check console for more info on why teleportation failed.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * When {@code firstJoinRtp} is enabled (set to true), this will RTP a player when they join the server
     * for the first time.
     *
     * @param event The PlayerJoinEvent
     */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (!firstJoinRtp || event.getPlayer().hasPlayedBefore()) return;
        try {
            event.getPlayer().teleport(
                    getRtpLocation(
                            firstJoinSettings,
                            firstJoinWorld.getSpawnLocation()
                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
        //All conditions must be met to continue
        if (onDeathRtp &&
            (!onDeathRequirePermission || event.getPlayer().hasPermission("jakesrtp.rtpondeath")) &&
            (!onDeathRespectBeds || !(event.isBedSpawn()/* || event.isAnchorSpawn()*/))
        ) try {
            event.setRespawnLocation(
                    getRtpLocation(
                            onDeathSettings,
                            onDeathWorld.getSpawnLocation() /*TODO~Decide: Do I want the player's death location instead? */
                    ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


