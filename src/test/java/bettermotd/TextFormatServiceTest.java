package bettermotd;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatServiceTest {

    private final TextFormatService service = new TextFormatService();

    @Test
    void autoStrictDoesNotTreatMathLikeMiniMessage() {
        TextFormatService.ParseResult parsed = service.parseToComponentDetailed("1 < 2 > 0", ColorFormat.AUTO_STRICT);

        assertEquals(ColorFormat.AUTO_STRICT, parsed.usedFormat());
        assertEquals(Component.text("1 < 2 > 0"), parsed.component());
    }

    @Test
    void autoStrictDetectsRealMiniMessageTags() {
        TextFormatService.ParseResult parsed = service.parseToComponentDetailed("<bold>Hello</bold>", ColorFormat.AUTO_STRICT);

        assertEquals(ColorFormat.MINI_MESSAGE, parsed.usedFormat());
        assertEquals(Component.text("Hello").decorate(net.kyori.adventure.text.format.TextDecoration.BOLD), parsed.component());
    }

    @Test
    void autoDetectsLegacyAmpersand() {
        TextFormatService.ParseResult parsed = service.parseToComponentDetailed("&aGreen", ColorFormat.AUTO);

        assertEquals(ColorFormat.LEGACY_AMPERSAND, parsed.usedFormat());
    }
}
