package cn.sh1rocu.tacz.api.event;

import cn.sh1rocu.tacz.TaCZFabric;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

/**
 * Camera angles and field of view, both of which the gun animation state machine drives.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why.
 */
@Environment(EnvType.CLIENT)
public abstract class ViewportEvent extends BaseEvent {
    private final GameRenderer renderer;
    private final Camera camera;
    private final double partialTick;

    public static final Event<FovCallback> FOV = EventFactory.createArrayBacked(FovCallback.class, callbacks -> event -> {
        for (FovCallback callback : callbacks) {
            callback.post(event);
        }
    });

    public static final Event<CameraCallback> CAMERA = EventFactory.createWithPhases(CameraCallback.class, callbacks -> event -> {
        for (CameraCallback callback : callbacks) {
            callback.post(event);
        }
    }, new Identifier[]{TaCZFabric.HIGHEST, TaCZFabric.HIGH, Event.DEFAULT_PHASE, TaCZFabric.LOW, TaCZFabric.LOWEST});

    public ViewportEvent(GameRenderer renderer, Camera camera, double partialTick) {
        this.renderer = renderer;
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public GameRenderer getRenderer() {
        return this.renderer;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public double getPartialTick() {
        return this.partialTick;
    }

    public interface CameraCallback {
        void post(ComputeCameraAngles event);
    }

    public interface FovCallback {
        void post(ComputeFov event);
    }

    /**
     * Fired before the camera rotation is applied. Handlers add to the angles rather than
     * replacing them, so several of them compose — recoil on top of the state machine's own
     * camera animation, for instance.
     */
    public static class ComputeCameraAngles extends ViewportEvent {
        private float yaw;
        private float pitch;
        private float roll;

        public ComputeCameraAngles(Camera camera, double partialTick, float yaw, float pitch, float roll) {
            super(Minecraft.getInstance().gameRenderer, camera, partialTick);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public float getYaw() {
            return this.yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return this.pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getRoll() {
            return this.roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }
    }

    /**
     * Fired on the computed field of view. Scope magnification and the gun model's own FOV
     * modifier both hook this.
     */
    public static class ComputeFov extends ViewportEvent {
        private final boolean usedConfiguredFov;
        private double fov;

        public ComputeFov(GameRenderer renderer, Camera camera, double partialTick, double fov, boolean usedConfiguredFov) {
            super(renderer, camera, partialTick);
            this.usedConfiguredFov = usedConfiguredFov;
            this.fov = fov;
        }

        public double getFOV() {
            return this.fov;
        }

        public void setFOV(double fov) {
            this.fov = fov;
        }

        public boolean usedConfiguredFov() {
            return this.usedConfiguredFov;
        }
    }
}
