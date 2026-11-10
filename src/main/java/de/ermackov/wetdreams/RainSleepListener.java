package de.ermackov.wetdreams;

import io.papermc.paper.block.bed.BedRuleResult;
import io.papermc.paper.event.player.PlayerBedFailEnterEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.world.ClockTimeSkipEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.util.BoundingBox;

/**
 * Lets players sleep while it rains during the day.
 *
 * <p>Vanilla only allows sleeping when the sky is dark. Rain darkens the sky a little, but not enough.
 * A thunderstorm does, but only once it has fully built up. Three things have to be changed:
 * <ol>
 *     <li>entering the bed, which vanilla refuses because it is too bright,</li>
 *     <li>staying in the bed, because {@code Player.tick} wakes every sleeping player up as soon as it is
 *     too bright to sleep,</li>
 *     <li>optionally the night skip, which would otherwise jump to the next morning.</li>
 * </ol>
 */
final class RainSleepListener implements Listener {

    /** Same area vanilla searches for monsters around a bed. */
    private static final double MONSTER_RANGE_HORIZONTAL = 8.0;
    private static final double MONSTER_RANGE_VERTICAL = 5.0;

    /** Time of day in which beds work in clear weather (vanilla night). */
    private static final long NIGHT_START = 12542;
    private static final long NIGHT_END = 23460;

    /** The tick method that wakes players up when it is too bright. Paper runs with Mojang mappings. */
    private static final String PLAYER_CLASS = "net.minecraft.world.entity.player.Player";
    private static final String PLAYER_TICK = "tick";

    private static final StackWalker STACK_WALKER = StackWalker.getInstance();

    private final boolean skipToMorning;

    RainSleepListener(boolean skipToMorning) {
        this.skipToMorning = skipToMorning;
    }

    // Not ignoreCancelled: the event counts as cancelled whenever vanilla refuses the bed, which is the case to handle.
    // A plugin that really wants to keep the player out sets DENY, which is checked first.
    @EventHandler(priority = EventPriority.HIGH)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.useBed() == Event.Result.DENY) return;
        if (!isRefusedOnlyForLight(event.getBedEnterResult(), event.enterAction().canSleep())) return;
        if (!event.getBed().getWorld().hasStorm()) return;
        // Vanilla checks for monsters only after the light check, so it has not happened yet
        if (monstersNearby(event.getPlayer(), event.getBed())) return;

        event.setUseBed(Event.Result.ALLOW);
    }

    /** Shows the vanilla monster message instead of "You can sleep only at night" when monsters kept the player awake. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedFailEnter(PlayerBedFailEnterEvent event) {
        if (event.getFailReason() != PlayerBedFailEnterEvent.FailReason.NOT_POSSIBLE_NOW) return;
        if (event.enterAction().canSleep() != BedRuleResult.TOO_MUCH_LIGHT) return;
        if (!event.getBed().getWorld().hasStorm()) return;
        if (monstersNearby(event.getPlayer(), event.getBed())) {
            event.setMessage(Component.translatable("block.minecraft.bed.not_safe"));
        }
    }

    /**
     * Keeps rain sleepers in bed while it rains. Only the wake-up from {@code Player.tick} because of the light is
     * cancelled; leaving the bed with the button, being woken by the night skip or by damage still works.
     * Once the rain stops, the next wake-up goes through.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!event.getPlayer().getWorld().hasStorm()) return;
        if (calledFromPlayerTick()) {
            event.setCancelled(true);
        }
    }

    /**
     * The night skip after sleeping through rain during the day would jump to the next morning; keep the time instead.
     * Only applies to regular rain, not thunderstorms: a thunderstorm already makes the sky dark enough for vanilla
     * to allow sleeping, so it is handled like a normal night and the skip goes through.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (skipToMorning) return;
        if (event.getSkipReason() != ClockTimeSkipEvent.SkipReason.NIGHT_SKIP) return;
        World world = event.getWorld();
        if (!world.hasStorm() || world.isThundering() || isNight(world)) return;
        // Not cancelled: a cancelled skip would also keep everyone in bed
        event.setSkipAmount(0);
    }

    private static boolean isRefusedOnlyForLight(PlayerBedEnterEvent.BedEnterResult result, BedRuleResult canSleep) {
        return result == PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW && canSleep == BedRuleResult.TOO_MUCH_LIGHT;
    }

    private static boolean isNight(World world) {
        long time = world.getTime();
        return time >= NIGHT_START && time < NIGHT_END;
    }

    private static boolean monstersNearby(Player player, Block bed) {
        if (player.getGameMode() == GameMode.CREATIVE) return false;
        BoundingBox area = BoundingBox.of(bed.getLocation().add(0.5, 0.0, 0.5),
                MONSTER_RANGE_HORIZONTAL, MONSTER_RANGE_VERTICAL, MONSTER_RANGE_HORIZONTAL);
        return !bed.getWorld().getNearbyEntities(area, RainSleepListener::preventsRest).isEmpty();
    }

    /** Mirrors vanilla: every monster prevents sleeping, a zombified piglin only while it is angry. */
    private static boolean preventsRest(Entity entity) {
        if (entity instanceof PigZombie piglin) return piglin.isAngry();
        return entity instanceof Monster;
    }

    private static boolean calledFromPlayerTick() {
        return STACK_WALKER.walk(frames -> frames.anyMatch(frame ->
                frame.getClassName().equals(PLAYER_CLASS) && frame.getMethodName().equals(PLAYER_TICK)));
    }
}
