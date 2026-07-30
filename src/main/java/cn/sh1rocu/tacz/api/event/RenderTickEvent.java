package cn.sh1rocu.tacz.api.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

/**
 * Fired once per frame rather than once per tick. Animation interpolation and the crosshair
 * need frame resolution, which {@code ClientTickEvents} cannot give them.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why.
 */
@Environment(EnvType.CLIENT)
public class RenderTickEvent extends BaseEvent {
    private final Minecraft client;
    public final Phase phase;
    private final DeltaTracker.Timer timer;

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    });

    public RenderTickEvent(Minecraft client, Phase phase, DeltaTracker.Timer timer) {
        this.client = client;
        this.phase = phase;
        this.timer = timer;
    }

    public Minecraft getClient() {
        return this.client;
    }

    public DeltaTracker.Timer getTimer() {
        return this.timer;
    }

    public interface Callback {
        void post(RenderTickEvent event);
    }

    public enum Phase {
        START,
        END
    }
}
