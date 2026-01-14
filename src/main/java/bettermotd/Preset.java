package bettermotd;

import java.util.List;

public record Preset(
        String id,
        int weight,
        String icon,
        List<String> motd,
        List<String> motdFrames
) {
    public static Preset empty() {
        return new Preset("empty", 1, "icons/default.png", List.of("BetterMOTD", "1.21.x"), List.of());
    }
}
