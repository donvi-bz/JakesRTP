package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

public class RtpOnEvent implements Listener {

    private final RandomTeleporter randomTeleporter;

    public RtpOnEvent(RandomTeleporter randomTeleporter) {this.randomTeleporter = randomTeleporter;}

    /**
     * When {@code firstJoinRtp} is enabled, this will RTP a player when they join the server
     * for the first time.
     *
     * @param event The PlayerJoinEvent
     */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (!randomTeleporter.firstJoinRtp || event.getPlayer().hasPlayedBefore()) return;
        try {
            assert randomTeleporter.firstJoinWorld != null;
            new RandomTeleportAction(
                    randomTeleporter, randomTeleporter.firstJoinSettings,
                    randomTeleporter.firstJoinWorld.getSpawnLocation(),
                    true, true,
                    randomTeleporter.logRtpOnPlayerJoin, "Rtp-on-join triggered!"
            ).teleportSync(event.getPlayer());
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
        if (randomTeleporter.onDeathRtp &&
            (!randomTeleporter.onDeathRequirePermission || event.getPlayer().hasPermission("jakesrtp.rtpondeath")) &&
            (!randomTeleporter.onDeathRespectBeds || !(event.isBedSpawn()/* || event.isAnchorSpawn()*/))
        ) try {
            assert randomTeleporter.onDeathWorld != null;
            event.setRespawnLocation(
                    new RandomTeleportAction(
                            randomTeleporter, randomTeleporter.onDeathSettings,
                            //TODO~Decide: Do I want the player's death location instead?
                            randomTeleporter.onDeathWorld.getSpawnLocation(),
                            true, true,
                            randomTeleporter.logRtpOnRespawn, "Rtp-on-respawn triggered!"
                    ).requestLocation()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
