package net.runelite.client.plugins.npcregentimer;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

class MemorizedNpc
{
    @Getter
    private int npcIndex;

    @Getter
    private String npcName;

    @Getter
    private int npcSize;

    /**
     * The time the npc died at, in game ticks, relative to the tick counter
     */
    @Getter
    @Setter
    private int diedOnTick;

    /**
     * The time it takes for the npc to respawn, in game ticks
     */
    @Getter
    @Setter
    private int respawnTime;

    @Getter
    @Setter
    private List<WorldPoint> possibleRespawnLocations;

    MemorizedNpc(NPC npc)
    {
        this.npcName = npc.getName();
        this.npcIndex = npc.getIndex();
        this.possibleRespawnLocations = new ArrayList<>();
        this.respawnTime = -1;
        this.diedOnTick = -1;

        final NPCComposition composition = npc.getTransformedComposition();

        if (composition != null)
        {
            this.npcSize = composition.getSize();
        }
    }
}