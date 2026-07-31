package com.tacz.guns.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputePlayerAimStateEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.tacz.guns.api.item.IGun;

/**
 * 5.x 把插件入口从注册器换成了事件总线：以前是
 * {@code register(IShoulderSurfingRegistrar)} 里挂一串回调，现在是
 * {@code register(IEventBus)} 里挂一串事件处理器。
 * <p>
 * 原来的 registerAdaptiveItemCallback（判断「手里这件东西要不要进瞄准姿态」）对应
 * ComputePlayerAimStateEvent —— 一样是给一个布尔结果，只是通过事件回传。
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    @Override
    public void register(IEventBus eventBus) {
        // 总线上每种事件都有一个同名的 register 重载，lambda 推断不出来，得写明处理器类型
        eventBus.register((ComputePlayerAimStateEventHandler) event -> {
            if (event.getEntity().getMainHandItem().getItem() instanceof IGun) {
                event.setResult(true);
            }
        });
    }
}
