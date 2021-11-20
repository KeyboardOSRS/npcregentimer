package net.runelite.client.plugins.npcregentimer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("npcregentimer")
public interface NpcRegenTimerConfig extends Config
{
    @ConfigItem(
            keyName = "npcRegenTicks",
            name = "Ticks per regen",
            description = "The number of ticks it takes for the npcs hp to regenerate.",
            position = 1
    )
    default int regenTickCount()
    {
        return 100;
    }

    @ConfigItem(
            keyName = "npcDeathAnimationTicks",
            name = "Ticks per death animation",
            description = "The number of ticks it takes for the npc to despawn after being hit the final damage.",
            position = 2
    )
    default int deathAnimTickCount()
    {
        return 3;
    }

    @ConfigItem(
            keyName = "incrementRegenTimer",
            name = "+1 Timer (single click)",
            description = "Increments the npcs hp regeneration timer.",
            position = 3
    )
    default boolean incrementCounterButton(){ return false; }

    @ConfigItem(
            keyName = "decrementRegenTimer",
            name = "-1 Timer (single click)",
            description = "Decrements the npcs hp regeneration timer.",
            position = 4
    )
    default boolean decrementCounterButton(){ return false; }

    @ConfigItem(
            keyName = "incrementFiveRegenTimer",
            name = "+5 Timer (single click)",
            description = "Increments the npcs hp regeneration timer by 5.",
            position = 5
    )
    default boolean incrementFiveCounterButton(){ return false; }

    @ConfigItem(
            keyName = "decrementFiveRegenTimer",
            name = "-5 Timer (single click)",
            description = "Decrements the npcs hp regeneration timer by 5.",
            position = 6
    )
    default boolean decrementFiveCounterButton(){ return false; }

    @ConfigItem(
            keyName = "resetRegenTimer",
            name = "Reset Timer (single click resets)",
            description = "Reset the npcs hp regeneration timer.",
            position = 7
    )
    default boolean resetCounterButton(){ return false; }


}
