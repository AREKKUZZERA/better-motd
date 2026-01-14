package bettermotd;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ConfigModel(
        String selectionMode,
        int stickyTtlSeconds,
        boolean animationEnabled,
        long frameIntervalMillis,
        List<Preset> presets
) {
    public static ConfigModel empty() {
        return new ConfigModel(
                "STICKY_PER_IP",
                10,
                true,
                450L,
                Collections.emptyList()
        );
    }

    public static ConfigModel from(FileConfiguration cfg) {
        if (cfg == null) return empty();

        String mode = cfg.getString("selectionMode", "STICKY_PER_IP");
        int ttl = cfg.getInt("stickyTtlSeconds", 10);

        boolean anim = cfg.getBoolean("animation.enabled", true);
        long interval = cfg.getLong("animation.frameIntervalMillis", 450L);

        List<Preset> presets = new ArrayList<>();

        // Если presets отсутствует или не список — просто будет пусто (без ошибок)
        var list = cfg.getMapList("presets");
        for (var raw : list) {
            if (!(raw instanceof java.util.Map<?, ?> m)) continue;

            String id = str(m.get("id"), "preset");
            int weight = intv(m.get("weight"), 1);

            String icon = str(m.get("icon"), null);
            List<String> iconFrames = strList(m.get("iconFrames"));

            List<String> motd = strList(m.get("motd"));
            List<String> motdFrames = strList(m.get("motdFrames"));

            presets.add(new Preset(id, weight, icon, iconFrames, motd, motdFrames));
        }

        return new ConfigModel(mode, ttl, anim, interval, presets);
    }

    private static String str(Object o, String def) {
        if (o == null) return def;
        String s = String.valueOf(o);
        return s.isBlank() ? def : s;
    }

    private static int intv(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static List<String> strList(Object o) {
        if (o == null) return Collections.emptyList();
        if (o instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object it : list) out.add(String.valueOf(it));
            return out;
        }
        return Collections.emptyList();
    }
}