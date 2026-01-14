package bettermotd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MotdService {

    private final JavaPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final IconCache iconCache;

    private volatile ConfigModel config = ConfigModel.empty();
    private final Map<String, StickyEntry> sticky = new ConcurrentHashMap<>();

    public MotdService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.iconCache = new IconCache(plugin);
    }

    public void reload() {
        this.config = ConfigModel.from(plugin.getConfig());
        iconCache.reload(config);
        sticky.clear();
    }

    public void shutdown() {
        sticky.clear();
        iconCache.clear();
    }

    public void apply(PaperServerListPingEvent event) {
        Preset preset = pickPreset(event.getAddress());
        long now = System.currentTimeMillis();

        event.motd(buildMotd(preset, now));
        event.setServerIcon(iconCache.pickIcon(preset));
    }

    @SuppressWarnings("deprecation")
    public void apply(ServerListPingEvent event) {
        Preset preset = pickPreset(event.getAddress());
        long now = System.currentTimeMillis();

        event.setMotd(buildPlainMotd(preset, now));
    }

    private Preset pickPreset(InetAddress addr) {
        List<Preset> presets = config.presets();
        if (presets.isEmpty())
            return Preset.empty();

        String mode = config.selectionMode();
        if ("RANDOM".equalsIgnoreCase(mode) || addr == null) {
            return weightedRandom(presets, System.nanoTime());
        }

        String key = addr.getHostAddress();
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, config.stickyTtlSeconds()) * 1000L;

        StickyEntry existing = sticky.get(key);
        if (existing != null && (now - existing.createdAtMs) <= ttlMs) {
            return existing.preset;
        }

        Preset chosen = weightedRandom(presets, (key.hashCode() * 31L) ^ now);
        sticky.put(key, new StickyEntry(chosen, now));
        return chosen;
    }

    private Preset weightedRandom(List<Preset> presets, long seed) {
        int total = 0;
        for (Preset p : presets)
            total += Math.max(1, p.weight());

        int r = (int) Math.floorMod(seed, total);

        int acc = 0;
        for (Preset p : presets) {
            acc += Math.max(1, p.weight());
            if (r < acc)
                return p;
        }
        return presets.get(0);
    }

    private Component buildMotd(Preset preset, long nowMs) {
        String frame = pickMotdFrame(preset, nowMs);
        return mini.deserialize(frame);
    }

    private String buildPlainMotd(Preset preset, long nowMs) {
        String frame = pickMotdFrame(preset, nowMs);
        return frame.replaceAll("<[^>]+>", "");
    }

    private String pickMotdFrame(Preset preset, long nowMs) {
        boolean anim = config.animationEnabled();

        List<String> frames = preset.motdFrames();
        if (anim && frames != null && !frames.isEmpty()) {
            int idx = (int) ((nowMs / config.frameIntervalMillis()) % frames.size());
            return frames.get(idx);
        }

        List<String> lines = preset.motd();
        if (lines == null || lines.isEmpty())
            return "BetterMOTD\n1.21.x";

        if (lines.size() == 1)
            return lines.get(0);
        return lines.get(0) + "\n" + lines.get(1);
    }

    private static final class StickyEntry {
        final Preset preset;
        final long createdAtMs;

        StickyEntry(Preset preset, long createdAtMs) {
            this.preset = preset;
            this.createdAtMs = createdAtMs;
        }
    }
}
