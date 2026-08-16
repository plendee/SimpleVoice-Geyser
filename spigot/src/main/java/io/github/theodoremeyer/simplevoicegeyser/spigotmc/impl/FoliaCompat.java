package io.github.theodoremeyer.simplevoicegeyser.spigotmc.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Small compatibility layer so the plugin can schedule player-bound work
 * correctly on both classic Bukkit/Spigot/Paper servers (single primary
 * thread) and Folia (per-region threads, no primary thread).
 * <p>
 * Uses reflection to call Paper/Folia's {@code Entity#getScheduler()} /
 * {@code EntityScheduler#run(...)} so the {@code spigot} module can keep
 * compiling against plain {@code spigot-api} without a hard dependency on
 * Paper/Folia API jars.
 */
public final class FoliaCompat {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    private FoliaCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Runs {@code task} on the thread that owns {@code entity}.
     * <p>
     * On Folia this dispatches via the entity's region scheduler (retrying
     * automatically until the entity is scheduled; silently dropped if the
     * entity has since been removed/retired, mirroring Folia's own
     * semantics). On non-Folia servers this preserves the previous
     * behaviour: run inline if already on the primary thread, otherwise
     * hop onto it via the classic Bukkit scheduler.
     */
    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            runViaEntityScheduler(plugin, entity, task);
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private static void runViaEntityScheduler(Plugin plugin, Entity entity, Runnable task) {
        try {
            Object scheduler = Entity.class.getMethod("getScheduler").invoke(entity);

            Method run = scheduler.getClass().getMethod(
                    "run", Plugin.class, Consumer.class, Runnable.class);

            Consumer<Object> wrapped = scheduledTask -> task.run();

            run.invoke(scheduler, plugin, wrapped, (Runnable) null);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Server reports itself as Folia but is missing the entity scheduler API", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to schedule Folia entity task", e.getCause());
        }
    }
}
