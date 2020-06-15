package biz.donvi.jakesRTP;

import biz.donvi.evenDistribution.RandomCords;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.RtpRegionShape.*;

public class RandomTeleporter implements CommandExecutor {


    private final ArrayList<RtpSettings> rtpSettings;


    /**
     * Creating an instance of the RandomTeleporter object is required to be able to use the command.
     * On creation, all relevant parts of the config are loaded into memory.
     *
     * @param config The configurationSection that holds the relevant data for RTPing
     * @throws Exception A generic exception for any issue had when creating the object.
     *                   I have NOT made my own exceptions, but instead have written different messages.
     */
    public RandomTeleporter(ConfigurationSection config) throws Exception {
        rtpSettings = new ArrayList<>();
        for (String key : config.getKeys(false))
            if (key.startsWith("random-teleport-settings"))
                try {
                    ConfigurationSection configSection = config.getConfigurationSection(key);
                    String configName = key.substring("random-teleport-settings".length() + 1);
                    if (configSection.getBoolean("enabled"))
                        rtpSettings.add(new RtpSettings(
                                configSection,
                                configName));
                    else infoLog("Not loading config " + configName + " since it is marked disabled.");
                } catch (NullPointerException e) {
                    PluginMain.infoLog(
                            "Whoops! Something in the config wasn't right, " +
                            rtpSettings.size() + " configs have been loaded thus far.");
                }
    }


    /**
     * Gets the RtpSettings object that contains the settings for the given world.
     *
     * @param world World to get RTP settings for
     * @return The RtpSettings of that world
     * @throws Exception If the world does not exist.
     */
    public RtpSettings getWorldRtpSettings(World world) throws Exception {
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.getConfigWorlds())
                if (world.equals(settingWorld))
                    return settings;
        throw new NotPermittedException("RTP is not enabled in this world.");
    }


    /**
     * This method acts as a bridge between this Minecraft specific class and my evenDistribution package
     * by calling the appropriate method from the package, and forwarding the relevant configuration
     * settings that have been saved in memory.
     *
     * @return A random X and Z coordinate pair.
     * @throws Exception if a shape is not properly defined,
     *                   though realistic error checking beforehand should prevent this issue
     */
    private int[] getRtpXZ(RtpSettings rtpSettings) throws Exception {
        int maxRadius = rtpSettings.getMaxRadius();
        int minRadius = rtpSettings.getMinRadius();
        switch (rtpSettings.getRtpRegionShape()) {
            case SQUARE:
                return RandomCords.getRandXySquare(maxRadius, minRadius);
            case CIRCLE:
                return RandomCords.getRandXyCircle(maxRadius, minRadius);
            case RECTANGLE:
                //return getRtpXzRectangle(); //This will get un-commented once I write a method for rectangles
            default:
                throw new Exception("RTP Region shape not properly defined.");
        }
    }


    /**
     * Keeps getting potential teleport locations until one has been found.
     * A fail-safe is included to throw an exception if too many unsuccessful attempts have been made.
     *
     * @param player The player running the command. Used to find the world they are in, or current location.
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception Only two points of this code are expected to be able to throw an exception:
     *                   getWorldRtpSettings() will throw an exception if the world is not RTP enabled.
     *                   getRtpXZ() will throw an exception if the rtp shape is not defined.
     */
    public Location getRtpLocation(Player player) throws Exception {
        long timeStart = System.currentTimeMillis();
        World playerWorld = player.getWorld();
        //Note: RtpSettings.getWorldRtpSettings() provides a potential exist point as it can throw an exception.
        RtpSettings rtpSettings = getWorldRtpSettings(playerWorld);
        PluginMain.plugin.getLogger().log(Level.INFO, "Player used RTP. Finding location...");

        Location potentialRtpLocation;
        int randAttemptCount = 0;
        do {

            int[] xz = getRtpXZ(rtpSettings);
            int[] xzOffset;
            switch (rtpSettings.getCenterLocation()) {
                case PLAYER_LOCATION:
                    xzOffset = new int[]{
                            (int) player.getLocation().getX(),
                            (int) player.getLocation().getZ()};
                    break;
                case WORLD_SPAWN:
                    xzOffset = new int[]{
                            (int) playerWorld.getSpawnLocation().getX(),
                            (int) playerWorld.getSpawnLocation().getZ()};
                    break;
                case PRESET_VALUE:
                default:
                    xzOffset = rtpSettings.getCenterXz();
            }

            potentialRtpLocation = new Location(
                    playerWorld,
                    xz[0] + xzOffset[0],
                    255,
                    xz[1] + xzOffset[1]
            );

            randAttemptCount++;
            playerWorld.removePluginChunkTickets(PluginMain.plugin);
        } while (!tryMakeLocationSafe(potentialRtpLocation, rtpSettings));

        long timeElapsed = System.currentTimeMillis() - timeStart;
        infoLog("Location found in " + timeElapsed + " milliseconds after " + randAttemptCount + " attempt(s).");
        if (timeElapsed > 1000)
            infoLog("Note: long search times are mostly caused by the server generating and loading new areas.");
        return potentialRtpLocation;
    }

    /**
     * Checks the safety of a point for teleporting, and if it is not safe, will try to make it safe.
     * If it can not be made safe under the constraints given, it will return false.
     * THIS SHOULD NOT BE USED FOR ANY TELEPORT, ONLY RTP.
     * This method can, by default, move the player up to 32 blocks away to find a safe location.
     *
     * @param potentialLoc Location to try and make safe.
     * @param rtpSettings  The worlds rtp settings
     * @return True if or when the location is safe, False if it can not be made safe under the given constraints.
     */
    private boolean tryMakeLocationSafe(Location potentialLoc, RtpSettings rtpSettings) {
        int[] smallHops = rtpSettings.getSmallHops();
        boolean safe = false;
        int tryCount = 0;
        while (tryCount < smallHops[0] && !safe) {
            if (tryCount > 0) {
                int[] newPos = RandomCords.getRandXyCircle(smallHops[1], smallHops[2]);
                newPos[0] += potentialLoc.getX();
                newPos[1] += potentialLoc.getZ();
                potentialLoc.setX(newPos[0]);
                potentialLoc.setY(potentialLoc.getY() + 10);
                potentialLoc.setZ(newPos[1]);
            }

            int chunkX = potentialLoc.getChunk().getX();
            int chunkZ = potentialLoc.getChunk().getX();
            potentialLoc.getWorld().addPluginChunkTicket(chunkX, chunkZ, PluginMain.plugin);
            new BukkitRunnable() {
                @Override
                public void run() {
                    potentialLoc.getWorld().removePluginChunkTicket(chunkX, chunkZ, PluginMain.plugin);
                }
            }.runTaskLater(PluginMain.plugin, 60);


            infoLog("Checking safety of location" +
                    " (" + potentialLoc.getX() +
                    ", " + potentialLoc.getY() +
                    ", " + potentialLoc.getZ() +
                    ") in world " + potentialLoc.getWorld().getName() +
                    (tryCount == 0 ? "" :
                            " (Sub attempt " +
                            tryCount +
                            ")"
                    )
            );
//            //Unused
//            World w = potentialLoc.getWorld();
//            int x = (int) potentialLoc.getX();
//            int y = (int) potentialLoc.getY();
//            int z = (int) potentialLoc.getZ();


            safe = true;
            tryCount++;

            while (isSafeToBeIn(potentialLoc.getBlock().getType()) &&
                   potentialLoc.getY() > rtpSettings.getLowBound()
            ) potentialLoc.add(0, -1, 0);

            if (!isSafeToBeOn(potentialLoc.getBlock().getType()) ||
                potentialLoc.getY() <= rtpSettings.getLowBound() ||
                isInATree(potentialLoc)
            ) safe = false;

        }
        //Centering the player on the block, and teleporting them on TOP of the safe landing spot
        potentialLoc.add(0.5, 1, 0.5);
        return safe;
    }

    /**
     * Checks the given material against a <u>whitelist</u> of materials deemed to be "safe to be in"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    private boolean isSafeToBeIn(Material mat) {
        switch (mat) {
            case AIR:
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
            case SNOW:
            case FERN:
            case LARGE_FERN:
            case VINE:
            case GRASS:
            case TALL_GRASS:
                return true;
            case WATER:
            case LAVA:
            case CAVE_AIR:
            default:
                return false;
        }
    }

    /**
     * Checks the given material against a <u>blacklist</u> of materials deemed to be "safe to be on"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    private boolean isSafeToBeOn(Material mat) {
        switch (mat) {
            case LAVA:
            case WATER:
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case CACTUS:
                return false;
            case GRASS:
            case STONE:
            case DIRT:
            default:
                return true;
        }
    }

    /**
     * Checks if the location is in a tree. To be in a tree, you must both be on a log, and in leaves.
     *
     * @param loc The location to check.
     * @return True if the location is in a tree.
     */
    private boolean isInATree(Location loc) {
        boolean onLog = false;
        boolean inLeaves = false;
        switch (loc.getBlock().getType()) {
            case ACACIA_LOG:
            case BIRCH_LOG:
            case DARK_OAK_LOG:
            case JUNGLE_LOG:
            case OAK_LOG:
            case SPRUCE_LOG:
            case RED_MUSHROOM_BLOCK:   // Giant mushrooms are basically trees, right?
            case BROWN_MUSHROOM_BLOCK: // Giant mushrooms are basically trees, right?
                onLog = true;
        }
        switch (loc.clone().add(0, 1, 0).getBlock().getType()) {
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
            case MUSHROOM_STEM: // Giant mushrooms are basically trees, right?
                inLeaves = true;
        }
        return onLog && inLeaves;
    }

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // - - - - - - - - - - - - - - - - - - - - - - - - - -
            // - - - - If a player tries to RTP themselves - - - -
            // - - - - - - - - - - - - - - - - - - - - - - - - - -
            if (args.length == 0 && sender instanceof Player) {
                Player player = (Player) sender;
                long callTime = System.currentTimeMillis();

                CoolDownTracker coolDownTracker = getWorldRtpSettings(player.getWorld()).coolDown;

                if (player.hasPermission("jakesRtp.noCooldown") || coolDownTracker.check(player.getName())) {
                    player.teleport(getRtpLocation(player));
                    coolDownTracker.log(player.getName(), callTime);
                } else {
                    player.sendMessage("Need to wait for cooldown: " + coolDownTracker.timeLeftWords(player.getName()));
                }
            }
            // - - - - - - - - - - - - - - - - - - - - - - - - - - -
            // - - - - If a player tries to RTP someone else - - - -
            // - - - - - - - - - - - - - - - - - - - - - - - - - - -
            else if (args.length == 1 && sender.hasPermission("jakesRtp.others")) {
                Player playerToTp = sender.getServer().getPlayerExact(args[0]);
                if (playerToTp == null)
                    sender.sendMessage("Could not find player " + args[0]);
                else playerToTp.teleport(getRtpLocation(playerToTp));
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
}


