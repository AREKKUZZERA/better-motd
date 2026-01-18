package bettermotd;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class MiniMessageAdapter {

    private final Logger logger;
    private final Object miniMessage;
    private final Method deserializeMethod;
    private final Class<?> componentClass;
    private final AtomicBoolean warned = new AtomicBoolean();

    public MiniMessageAdapter(Logger logger) {
        this.logger = logger;
        Object mini = null;
        Method deserialize = null;
        Class<?> component = null;
        try {
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Method miniMessageFactory = miniMessageClass.getMethod("miniMessage");
            mini = miniMessageFactory.invoke(null);
            deserialize = miniMessageClass.getMethod("deserialize", String.class);
            component = Class.forName("net.kyori.adventure.text.Component");
        } catch (Exception e) {
            warnOnce("MiniMessage is not available. Falling back to plain text MOTD.");
        }
        this.miniMessage = mini;
        this.deserializeMethod = deserialize;
        this.componentClass = component;
    }

    public Object deserialize(String input) {
        if (miniMessage == null || deserializeMethod == null) {
            return null;
        }
        try {
            return deserializeMethod.invoke(miniMessage, input);
        } catch (Exception e) {
            warnOnce("Failed to deserialize MiniMessage text: " + e.getMessage());
            return null;
        }
    }

    public Class<?> componentClass() {
        return componentClass;
    }

    private void warnOnce(String message) {
        if (logger == null) {
            return;
        }
        if (warned.compareAndSet(false, true)) {
            logger.warning(message);
        }
    }
}
