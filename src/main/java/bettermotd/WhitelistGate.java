package bettermotd;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class WhitelistGate implements Listener {

    private static final AtomicBoolean COMPONENT_API_PROBED = new AtomicBoolean(false);
    private static volatile Method DISALLOW_COMPONENT_METHOD;
    private static volatile Method COMPONENT_TEXT_METHOD;

    private final Plugin plugin;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.disabled());

    private GateSettings settings = GateSettings.disabled();
    private BukkitTask refreshTask;

    public WhitelistGate(Plugin plugin) {
        this.plugin = plugin;
        probeComponentDisallowApi();
    }

    public void applyConfig(ConfigModel.WhitelistSettings whitelist) {
        if (whitelist == null) {
            this.settings = GateSettings.disabled();
        } else {
            int refreshSeconds = Math.max(1, whitelist.gateRefreshSeconds());
            String kickMessage = whitelist.gateKickMessage();
            if (kickMessage == null || kickMessage.isBlank()) {
                kickMessage = "You are not whitelisted.";
            }
            this.settings = new GateSettings(whitelist.gateEnabled(), refreshSeconds, kickMessage);
        }

        refreshSnapshot();
        rescheduleTask();
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        snapshot.set(Snapshot.disabled());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event == null || event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        Snapshot current = snapshot.get();
        if (!current.enforced()) {
            return;
        }

        UUID uuid = event.getUniqueId();
        if (uuid != null && current.allowedUuids().contains(uuid)) {
            return;
        }

        String name = event.getName();
        if (name != null && current.allowedNamesLower().contains(name.toLowerCase(Locale.ROOT))) {
            return;
        }

        disallow(event, AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, current.kickMessage());
    }

    private void rescheduleTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (!settings.enabled()) {
            snapshot.set(Snapshot.disabled());
            return;
        }

        long intervalTicks = Math.max(1L, settings.refreshSeconds()) * 20L;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshSnapshot, intervalTicks, intervalTicks);
    }

    private void refreshSnapshot() {
        snapshot.set(buildSnapshot(settings));
    }

    private Snapshot buildSnapshot(GateSettings gateSettings) {
        boolean enforced = gateSettings.enabled() && Bukkit.hasWhitelist();

        if (!enforced) {
            return new Snapshot(Set.of(), Set.of(), false, gateSettings.kickMessage());
        }

        Set<UUID> uuids = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
            if (player == null) {
                continue;
            }
            UUID uuid = player.getUniqueId();
            if (uuid != null) {
                uuids.add(uuid);
            }
            String name = player.getName();
            if (name != null && !name.isBlank()) {
                names.add(name.toLowerCase(Locale.ROOT));
            }
        }

        return new Snapshot(Set.copyOf(uuids), Set.copyOf(names), true, gateSettings.kickMessage());
    }

    private static void disallow(AsyncPlayerPreLoginEvent event,
            AsyncPlayerPreLoginEvent.Result result,
            String message) {
        Method disallowComponent = DISALLOW_COMPONENT_METHOD;
        Method componentText = COMPONENT_TEXT_METHOD;

        if (disallowComponent != null && componentText != null) {
            try {
                Object component = componentText.invoke(null, message);
                disallowComponent.invoke(event, result, component);
                return;
            } catch (Throwable ignored) {
            }
        } else {
            probeComponentDisallowApi();
            disallowComponent = DISALLOW_COMPONENT_METHOD;
            componentText = COMPONENT_TEXT_METHOD;
            if (disallowComponent != null && componentText != null) {
                try {
                    Object component = componentText.invoke(null, message);
                    disallowComponent.invoke(event, result, component);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }

        disallowLegacy(event, result, message);
    }

    @SuppressWarnings("deprecation")
    private static void disallowLegacy(AsyncPlayerPreLoginEvent event,
            AsyncPlayerPreLoginEvent.Result result,
            String message) {
        event.disallow(result, message);
    }

    private static void probeComponentDisallowApi() {
        if (!COMPONENT_API_PROBED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");

            Method text = componentClass.getMethod("text", String.class);

            Method disallow = AsyncPlayerPreLoginEvent.class.getMethod("disallow",
                    AsyncPlayerPreLoginEvent.Result.class, componentClass);

            COMPONENT_TEXT_METHOD = text;
            DISALLOW_COMPONENT_METHOD = disallow;
        } catch (Throwable ignored) {
            COMPONENT_TEXT_METHOD = null;
            DISALLOW_COMPONENT_METHOD = null;
        }
    }

    private record GateSettings(boolean enabled, int refreshSeconds, String kickMessage) {
        private static GateSettings disabled() {
            return new GateSettings(false, 5, "You are not whitelisted.");
        }
    }

    private record Snapshot(Set<UUID> allowedUuids,
            Set<String> allowedNamesLower,
            boolean enforced,
            String kickMessage) {
        private static Snapshot disabled() {
            return new Snapshot(Set.of(), Set.of(), false, "You are not whitelisted.");
        }
    }
}
