package bettermotd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterMOTDPlugin extends JavaPlugin {

    private MotdService motdService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.motdService = new MotdService(this);
        this.motdService.reload();

        getServer().getPluginManager().registerEvents(
                new ServerPingListener(motdService),
                this
        );

        getLogger().info("BetterMOTD enabled.");
    }

    @Override
    public void onDisable() {
        if (motdService != null) motdService.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("bettermotd")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bettermotd.reload")) {
                sender.sendMessage("No permission.");
                return true;
            }
            reloadConfig();
            motdService.reload();
            sender.sendMessage("BetterMOTD reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /bettermotd reload");
        return true;
    }
}
