package bettermotd;

import java.util.Collections;
import java.util.List;

public record Preset(
        String id,
        int weight,
        String icon,
        List<String> iconFrames,
        List<String> motd,
        List<String> motdFrames
) {
    public static Preset empty() {
        return new Preset(
                "empty",
                1,
                null,
                Collections.emptyList(),
                List.of("BetterMOTD", "1.21.x"),
                Collections.emptyList()
        );
    }
}
