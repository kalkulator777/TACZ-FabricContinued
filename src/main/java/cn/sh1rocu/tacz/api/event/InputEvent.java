package cn.sh1rocu.tacz.api.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.ApiStatus;

@Environment(EnvType.CLIENT)
public abstract class InputEvent extends BaseEvent {
    public static class InteractionKeyMappingTriggered extends InputEvent implements ICancellableEvent {
        public static final Event<IKMTCallback> EVENT = EventFactory.createArrayBacked(IKMTCallback.class, callbacks -> event -> {
            for (IKMTCallback e : callbacks) e.onInteractionKeyMappingTriggered(event);
        });

        private final int button;
        private final KeyMapping keyMapping;
        private final InteractionHand hand;
        private boolean handSwing = true;

        public InteractionKeyMappingTriggered(int button, KeyMapping keyMapping, InteractionHand hand) {
            this.button = button;
            this.keyMapping = keyMapping;
            this.hand = hand;
        }

        public void setSwingHand(boolean value) {
            this.handSwing = value;
        }

        public boolean shouldSwingHand() {
            return this.handSwing;
        }

        public InteractionHand getHand() {
            return this.hand;
        }

        public boolean isAttack() {
            return this.button == 0;
        }

        public boolean isUseItem() {
            return this.button == 1;
        }

        public boolean isPickBlock() {
            return this.button == 2;
        }

        public KeyMapping getKeyMapping() {
            return this.keyMapping;
        }
    }

    public static class Key extends InputEvent {
        public static final Event<KeyCallback> EVENT = EventFactory.createArrayBacked(KeyCallback.class, callbacks -> event -> {
            for (KeyCallback e : callbacks) e.onKey(event);
        });

        private final KeyEvent event;
        private final int action;

        @ApiStatus.Internal
        public Key(KeyEvent event, int action) {
            this.event = event;
            this.action = action;
        }

        /**
         * 1.21.9 把键盘输入收进了 {@link KeyEvent}，而 {@link KeyMapping#matches} 只认这个类型，
         * 所以事件带的是原版的记录本身，下面几个取值方法只是为了不动调用方。
         */
        public KeyEvent getKeyEvent() {
            return this.event;
        }

        public int getKey() {
            return this.event.key();
        }

        public int getScanCode() {
            return this.event.scancode();
        }

        public int getAction() {
            return this.action;
        }

        public int getModifiers() {
            return this.event.modifiers();
        }
    }

    public static class MouseButton extends InputEvent {
        public static final Event<MouseCallback> EVENT = EventFactory.createArrayBacked(MouseCallback.class, callbacks -> event -> {
            for (MouseCallback e : callbacks) e.onMouse(event);
        });

        private final MouseButtonEvent event;
        private final int action;

        @ApiStatus.Internal
        protected MouseButton(MouseButtonEvent event, int action) {
            this.event = event;
            this.action = action;
        }

        /**
         * 同 {@link Key#getKeyEvent()}：{@link KeyMapping#matchesMouse} 要的是原版记录。
         */
        public MouseButtonEvent getMouseButtonEvent() {
            return this.event;
        }

        public int getButton() {
            return this.event.button();
        }

        public int getAction() {
            return this.action;
        }

        public int getModifiers() {
            return this.event.modifiers();
        }

        public static class Post extends InputEvent.MouseButton {
            public static final Event<MousePostCallback> EVENT = EventFactory.createArrayBacked(MousePostCallback.class, callbacks -> event -> {
                for (MousePostCallback e : callbacks) e.onMousePost(event);
            });

            @ApiStatus.Internal
            public Post(MouseButtonEvent event, int action) {
                super(event, action);
            }
        }

        public static class Pre extends InputEvent.MouseButton implements ICancellableEvent {
            public static final Event<MousePreCallback> EVENT = EventFactory.createArrayBacked(MousePreCallback.class, callbacks -> event -> {
                for (MousePreCallback e : callbacks) e.onMousePre(event);
            });

            @ApiStatus.Internal
            public Pre(MouseButtonEvent event, int action) {
                super(event, action);
            }
        }
    }

    public interface IKMTCallback {
        void onInteractionKeyMappingTriggered(InteractionKeyMappingTriggered event);
    }

    public interface KeyCallback {
        void onKey(Key event);
    }

    public interface MouseCallback {
        void onMouse(MouseButton event);
    }

    public interface MousePostCallback {
        void onMousePost(MouseButton.Post event);
    }

    public interface MousePreCallback {
        void onMousePre(MouseButton.Pre event);
    }
}
