package bettermotd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public final class ServerPingListener implements Listener {

    private final MotdService service;

    public ServerPingListener(MotdService service) {
        this.service = service;
    }

    @EventHandler
    public void onPaperPing(PaperServerListPingEvent event) {
        service.apply(event);
    }

    @EventHandler
    public void onBukkitPing(ServerListPingEvent event) {
        service.apply(event);
    }
}
