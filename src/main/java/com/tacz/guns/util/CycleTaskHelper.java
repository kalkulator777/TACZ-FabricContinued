package com.tacz.guns.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class CycleTaskHelper {
    private static final List<CycleTaskTicker> CYCLE_TASKS = new LinkedList<>();
    private static final List<CycleTaskTicker> TEMP_CYCLE_TASKS = new LinkedList<>();

    /**
     * 根据提供的时间间隔循环执行任务。会立刻调用一次。
     *
     * @param task     循环执行的任务，会根据返回的 boolean 值决定是否继续下一次循环。如果返回 false ，则将不再循环。
     * @param periodMs 循环调用的时间间隔，单位为毫秒。
     * @param cycles   最大循环次数。-1 代表无限次。
     */
    public static void addCycleTask(BooleanSupplier task, long periodMs, int cycles) {
        CycleTaskTicker ticker = new CycleTaskTicker(task, periodMs, cycles);
        if (ticker.tick()) {
            CYCLE_TASKS.add(ticker);
        }
    }

    public static void addCycleTask(BooleanSupplier task, long delayMs, long periodMs, int cycles) {
        if (delayMs <= 0) {
            addCycleTask(task, periodMs, cycles);
            return;
        }
        CycleTaskTicker ticker = new CycleTaskTicker(task, delayMs, periodMs, cycles);
        CYCLE_TASKS.add(ticker);
    }

    public static void tick() {
        TEMP_CYCLE_TASKS.addAll(CYCLE_TASKS);
        CYCLE_TASKS.clear();
        TEMP_CYCLE_TASKS.removeIf(ticker -> !ticker.tick());
        CYCLE_TASKS.addAll(TEMP_CYCLE_TASKS);
        TEMP_CYCLE_TASKS.clear();
    }

    private static class CycleTaskTicker {
        private final BooleanSupplier task;
        private final float periodS;
        private final int cycles;
        private float delayS = 0;
        private long timestamp = -1;
        private float compensation = 0;
        private int count = 0;

        private CycleTaskTicker(BooleanSupplier task, long periodMs, int cycles) {
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
        }

        private CycleTaskTicker(BooleanSupplier task, long delayMs, long periodMs, int cycles) {
            this.delayS = delayMs / 1000f;
            this.timestamp = System.currentTimeMillis();
            this.task = task;
            this.periodS = periodMs / 1000f;
            this.cycles = cycles;
        }

        private boolean tick() {
            if (timestamp == -1) {
                timestamp = System.currentTimeMillis();
                if (cycles > 0 && ++count > cycles) {
                    return false;
                }
                return task.getAsBoolean();
            }
            float duration = (System.currentTimeMillis() - timestamp) / 1000f + compensation;
            if (delayS > 0) {
                /* duration 是从构造那一刻算起的总时长，而延迟路径上 timestamp 从来不往前挪，
                 * 所以原来每 tick 都从 delayS 里扣掉一整段「至今为止的时间」—— 倒计时按 tick
                 * 数平方地缩短，1 秒的延迟六个 tick 就到期了。而且到期那一支里 delayS 已经被
                 * 置 0，紧接着的 duration - delayS 什么也没减掉，整段延迟时间被当成欠账，
                 * 下面的 while 会把 delay/period 次调用一口气补完。
                 * 延迟到期就干脆利落地跑一次，然后从此刻重新起算周期。*/
                if (duration < delayS) {
                    return true;
                }
                delayS = 0;
                compensation = 0;
                timestamp = System.currentTimeMillis();
                if (cycles > 0 && ++count > cycles) {
                    return false;
                }
                return task.getAsBoolean();
            }
            if (duration > periodS) {
                compensation = duration;
                timestamp = System.currentTimeMillis();
                while (compensation > periodS) {
                    if (cycles > 0 && ++count > cycles) {
                        return false;
                    }
                    if (!task.getAsBoolean()) {
                        return false;
                    }
                    compensation -= periodS;
                }
            }
            return true;
        }
    }
}
