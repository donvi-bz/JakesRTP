package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

public class RtpOnEvent implements Listener {

    private final RandomTeleporter rt;

    public RtpOnEvent(RandomTeleporter rt) {this.rt = rt;}

    /**
     * When {@code firstJoinRtp} is enabled, this will RTP a player when they join the server
     * for the first time.
     *
     * @param event The PlayerJoinEvent
     */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (!rt.firstJoinRtp || event.getPlayer().hasPlayedBefore()) return;
        try {
            long startTime = System.currentTimeMillis();

            assert rt.firstJoinSettings != null;
            assert rt.firstJoinWorld != null;
            Location rtpLocation = rt.getRtpLocation(
                    rt.firstJoinSettings,
                    rt.firstJoinWorld.getSpawnLocation(),
                    true);
            event.getPlayer().teleport(rtpLocation);

            long endTime = System.currentTimeMillis();
            if (rt.logRtpOnPlayerJoin) infoLog(
                    "Rtp-on-join triggered! " +
                    "Teleported player " + event.getPlayer().getName() +
                    " to " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                    " taking " + (endTime - startTime) + " ms.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * When {@code onDeathRtp} is enabled, this will RTP a player when they die (with a few exceptions).<p>
     * A player will not be teleported randomly, even if {@code onDeathRtp} is true, if at least one of these conditions is true:<p>
     * • {@code onDeathRequirePermission} is true, and the player does not have the correct permission<p>
     * • {@code onDeathRespectBeds} is true, and the player has a home bed
     *
     * @param event
     */
    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
        //All conditions must be met to continue
        if (rt.onDeathRtp &&
            (!rt.onDeathRequirePermission || event.getPlayer().hasPermission("jakesrtp.rtpondeath")) &&
            (!rt.onDeathRespectBeds || !(event.isBedSpawn()/* || event.isAnchorSpawn()*/))
        ) try {
            long startTime = System.currentTimeMillis();
            //TODO~Decide: Do I want the player's death location instead?
            assert rt.onDeathSettings != null;
            assert rt.onDeathWorld != null;
            Location rtpLocation = rt.getRtpLocation(
                    rt.onDeathSettings,
                    rt.onDeathWorld.getSpawnLocation(),
                    true);
            event.setRespawnLocation(rtpLocation);

            long endTime = System.currentTimeMillis();
            if (rt.logRtpOnRespawn) infoLog(
                    "Rtp-on-respawn triggered! " +
                    "Teleported player " + event.getPlayer().getName() +
                    " to " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                    " taking " + (endTime - startTime) + " ms.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
