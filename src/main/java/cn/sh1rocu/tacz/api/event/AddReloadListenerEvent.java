package cn.sh1rocu.tacz.api.event;

import com.google.common.collect.ImmutableList;
import com.tacz.guns.GunMod;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AddReloadListenerEvent extends BaseEvent {
    private final List<PreparableReloadListener> listeners = new ArrayList<>();
    private final ReloadableServerResources serverResources;
    private final RegistryAccess registryAccess;

    public static Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class, callbacks -> ((event) -> {
        for (Callback e : callbacks) {
            e.post(event);
        }
    }));

    public interface Callback {
        void post(AddReloadListenerEvent event);
    }

    /**
     * Fires the event and returns everything the listeners registered.
     */
    public static List<PreparableReloadListener> gatherListeners(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
        AddReloadListenerEvent event = new AddReloadListenerEvent(serverResources, registryAccess);
        CALLBACK.invoker().post(event);
        return event.getListeners();
    }

    public AddReloadListenerEvent(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
        this.serverResources = serverResources;
        this.registryAccess = registryAccess;
    }

    public ReloadableServerResources getServerResources() {
        return serverResources;
    }

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }

    public void addListener(PreparableReloadListener listener) {
        listeners.add(new WrappedStateAwareListener(listener));
    }

    public List<PreparableReloadListener> getListeners() {
        return ImmutableList.copyOf(listeners);
    }

    private static class WrappedStateAwareListener implements PreparableReloadListener {
        private final PreparableReloadListener wrapped;

        private WrappedStateAwareListener(final PreparableReloadListener wrapped) {
            this.wrapped = wrapped;
        }

        /**
         * 1.21.11 把重载的六个参数收成了四个：资源管理器和两个 profiler 都进了 SharedState。
         */
        @Override
        public CompletableFuture<Void> reload(final SharedState state, final Executor backgroundExecutor, final PreparationBarrier barrier, final Executor gameExecutor) {
            if (FabricLoader.getInstance().isModLoaded(GunMod.MOD_ID))
                return wrapped.reload(state, backgroundExecutor, barrier, gameExecutor);
            else
                return CompletableFuture.completedFuture(null);
        }
    }
}