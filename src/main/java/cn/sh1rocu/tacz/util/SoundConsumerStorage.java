package cn.sh1rocu.tacz.util;

import com.mojang.blaze3d.audio.Channel;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

// From Kilt
public class SoundConsumerStorage {
    // The sound engine is lambda hell, and so we have to use this storage to be able to ensure that the channel access execute inject in
    // ChannelAccessHandleMixin will actually be run in the correct places. We also can't wrap the consumer, because otherwise,
    // some other mod that tries to do the same thing will cause either us or them to fail.
    /* 弱引用集合。取出这个 consumer 的地方只有 ChannelAccessHandleMixin，而那段注入挂在
     * ChannelHandle.execute 的 lambda 里，原版只有在 channel 还在的时候才会跑它 —— 声道要是
     * 先被 release 掉（换资源包、停掉全部声音，ChannelAccess.clear 一次就是一批），排队的任务
     * 什么也不做，条目就永远留下了，连带 lambda 捕获的 SoundBuffer 和 ChannelHandle。
     * consumer 在真正被用到之前，一直由排队的任务强引用着，所以弱引用不会提前失效；任务一没，
     * 条目自己就走了。 */
    public static final Set<Consumer<Channel>> soundConsumerChannels =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
}