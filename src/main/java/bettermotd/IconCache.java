package bettermotd;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IconCache {

    private static final String DEFAULT_ICON_RESOURCE = "icons/default.png";
    private static final String DEFAULT_ICON_TARGET = "icons/default.png";

    private final JavaPlugin plugin;
    private final Map<String, CachedServerIcon> cache = new ConcurrentHashMap<>();

    private volatile boolean animEnabled = true;
    private volatile long frameIntervalMs = 450L;

    private File iconsDir;

    public IconCache(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(ConfigModel cfg) {
        this.animEnabled = cfg.animationEnabled();
        this.frameIntervalMs = cfg.frameIntervalMillis();
        cache.clear();

        ensureIconsDirectory();
        ensureDefaultIconIfNoPngs();
        warnIfIconsEmpty(); // после попытки создать заглушку
    }

    public void clear() {
        cache.clear();
    }

    public CachedServerIcon pickIcon(Preset preset, long nowMs) {
        List<String> frames = preset.iconFrames();
        if (animEnabled && frames != null && !frames.isEmpty()) {
            int idx = (int) ((nowMs / frameIntervalMs) % frames.size());
            return loadIcon(frames.get(idx));
        }

        String single = preset.icon();
        if (single == null || single.isBlank()) return null;
        return loadIcon(single);
    }

    /* =========================
       Internal helpers
       ========================= */

    private void ensureIconsDirectory() {
        this.iconsDir = new File(plugin.getDataFolder(), "icons");

        if (!iconsDir.exists()) {
            if (iconsDir.mkdirs()) {
                plugin.getLogger().info("Created icons directory: " + iconsDir.getPath());
            } else {
                plugin.getLogger().warning("Failed to create icons directory: " + iconsDir.getPath());
            }
        }
    }

    private void ensureDefaultIconIfNoPngs() {
        if (iconsDir == null || !iconsDir.isDirectory()) return;

        File[] pngs = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngs != null && pngs.length > 0) {
            return; // уже есть png — заглушка не нужна
        }

        File target = new File(plugin.getDataFolder(), DEFAULT_ICON_TARGET);
        if (target.exists()) {
            return; // по какой-то причине файл уже есть
        }

        boolean ok = copyResourceToFile(DEFAULT_ICON_RESOURCE, target);
        if (ok) {
            plugin.getLogger().info("Created default icon: " + target.getPath());
        } else {
            plugin.getLogger().warning(
                    "No icons found and default icon resource is missing. " +
                    "Add a 64x64 PNG at: " + target.getPath()
            );
        }
    }

    private void warnIfIconsEmpty() {
        if (iconsDir == null || !iconsDir.isDirectory()) return;

        File[] pngs = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngs == null || pngs.length == 0) {
            plugin.getLogger().warning(
                    "Icons directory is empty. Server list icon will not be shown. " +
                    "Place 64x64 PNG files into: " + iconsDir.getPath()
            );
        }
    }

    private boolean copyResourceToFile(String resourcePath, File target) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return false;

            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Failed to create directory: " + parent.getPath());
                return false;
            }

            try (FileOutputStream out = new FileOutputStream(target)) {
                in.transferTo(out);
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to write default icon: " + e.getMessage());
            return false;
        }
    }

    private CachedServerIcon loadIcon(String relPath) {
        if (relPath == null || relPath.isBlank()) return null;

        return cache.computeIfAbsent(relPath, key -> {
            try {
                File file = new File(plugin.getDataFolder(), key);
                if (!file.exists()) {
                    plugin.getLogger().warning("Icon not found: " + file.getPath());
                    return null;
                }

                Server server = Bukkit.getServer();
                return server.loadServerIcon(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load icon '" + relPath + "': " + e.getMessage());
                return null;
            }
        });
    }
}
