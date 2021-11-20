package net.runelite.client.plugins.npcregentimer;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;

@PluginDescriptor(
        name = "Npc Regen Timer",
        description = "Show hitpoint regen timer for a tagged NPC",
        enabledByDefault = false
)
public class NpcRegenTimerPlugin extends Plugin
{
    private static final int MAX_ACTOR_VIEW_RANGE = 15;

    // Option added to NPC menu
    private static final String TAG = "Regen";

    private static final List<MenuAction> NPC_MENU_ACTIONS = ImmutableList.of(MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
            MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION);

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NpcRegenTimerConfig config;

    @Inject
    private NpcRegenTimerInput inputListener;

    @Inject
    private NpcRegenTimerOverlay npcRegenTimerOverlay;

    /**
     * Tagged NPCs that spawned this tick, which need to be verified that
     * they actually spawned and didn't just walk into view range.
     */
    private final List<NPC> spawnedNpcsThisTick = new ArrayList<>();

    /**
     * Tagged NPCs that despawned this tick, which need to be verified that
     * they actually spawned and didn't just walk into view range.
     */
    private final List<NPC> despawnedNpcsThisTick = new ArrayList<>();

    /**
     * World locations of graphics object which indicate that an
     * NPC teleported that were played this tick.
     */
    private final Set<WorldPoint> teleportGraphicsObjectSpawnedThisTick = new HashSet<>();

    /**
     * The players location on the last game tick.
     */
    private WorldPoint lastPlayerLocation;

    /**
     * When hopping worlds, NPCs can spawn without them actually respawning,
     * so we would not want to mark it as a real spawn in those cases.
     */
    private boolean skipNextSpawnCheck = false;

    @Provides
    NpcRegenTimerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NpcRegenTimerConfig.class);
    }

    @Setter(AccessLevel.PACKAGE)
    private boolean hotKeyPressed = false;

    @Getter(AccessLevel.PACKAGE)
    private MemorizedNpc memorizedNpc;

    @Getter(AccessLevel.PACKAGE)
    private Integer tickCountdown;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(npcRegenTimerOverlay);
        keyManager.registerKeyListener(inputListener);
        clientThread.invoke(() ->
        {
            skipNextSpawnCheck = true;
        });
        tickCountdown = config.regenTickCount();
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(npcRegenTimerOverlay);
        memorizedNpc = null;
        spawnedNpcsThisTick.clear();
        despawnedNpcsThisTick.clear();
        teleportGraphicsObjectSpawnedThisTick.clear();
        tickCountdown = null;
        keyManager.unregisterKeyListener(inputListener);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN ||
                event.getGameState() == GameState.HOPPING)
        {
            if (memorizedNpc != null) {
                memorizedNpc.setDiedOnTick(-1);
            }
            lastPlayerLocation = null;
            skipNextSpawnCheck = true;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged)
    {
        if (configChanged.getGroup().equals("npcregentimer"))
        {
            if (configChanged.getKey().equals("npcRegenTicks"))
            {
                tickCountdown = config.regenTickCount();
            }
            else if (configChanged.getKey().equals("incrementRegenTimer"))
            {
                tickCountdown = tickCountdown + 1;
            }
            else if (configChanged.getKey().equals("decrementRegenTimer"))
            {
                if (tickCountdown > 0)
                {
                    tickCountdown = tickCountdown - 1;
                }
            }
            else if (configChanged.getKey().equals("incrementFiveRegenTimer"))
            {
                tickCountdown = tickCountdown + 5;
            }
            else if (configChanged.getKey().equals("decrementFiveRegenTimer"))
            {
                if (tickCountdown > 5)
                {
                    tickCountdown = tickCountdown - 5;
                }
            }
            else if (configChanged.getKey().equals("resetRegenTimer"))
            {
                tickCountdown = config.regenTickCount();
            }
        }
    }

    @Subscribe
    public void onFocusChanged(FocusChanged focusChanged)
    {
        if (!focusChanged.isFocused())
        {
            hotKeyPressed = false;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (!hotKeyPressed || event.getType() != MenuAction.EXAMINE_NPC.getId())
        {
            return;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
        MenuEntry menuEntry = menuEntries[menuEntries.length - 1] = new MenuEntry();
        menuEntry.setOption(TAG);
        menuEntry.setTarget(event.getTarget());
        menuEntry.setParam0(event.getActionParam0());
        menuEntry.setParam1(event.getActionParam1());
        menuEntry.setIdentifier(event.getIdentifier());
        menuEntry.setType(MenuAction.RUNELITE.getId());
        client.setMenuEntries(menuEntries);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked click)
    {
        if (click.getMenuAction() != MenuAction.RUNELITE || !click.getMenuOption().equals(TAG))
        {
            return;
        }

        final int id = click.getId();
        final NPC[] cachedNPCs = client.getCachedNPCs();
        final NPC npc = cachedNPCs[id];

        if (npc == null || npc.getName() == null)
        {
            return;
        }

        if (memorizedNpc != null) {
            memorizedNpc = null;
            tickCountdown = config.regenTickCount();
        }
        else {
            memorizeNpc(npc);
        }

        click.consume();
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned)
    {
        final NPC npc = npcSpawned.getNpc();
        final String npcName = npc.getName();

        if (npcName == null)
        {
            return;
        }

        if (memorizedNpc != null && memorizedNpc.getNpcIndex() == npc.getIndex()) {
            memorizeNpc(npc);
            spawnedNpcsThisTick.add(npc);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned)
    {
        final NPC npc = npcDespawned.getNpc();

        if (memorizedNpc != null && memorizedNpc.getNpcIndex() == npc.getIndex())
        {
            despawnedNpcsThisTick.add(npc);
            tickCountdown = tickCountdown + (config.deathAnimTickCount() - 2);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        //removeOldDeadNpc();
        validateSpawnedNpcs();
        lastPlayerLocation = client.getLocalPlayer().getWorldLocation();

        if (memorizedNpc != null &&  memorizedNpc.getDiedOnTick() == -1) {
            if (tickCountdown > 1) {
                tickCountdown = tickCountdown - 1;
            }
            else {
                tickCountdown = config.regenTickCount();
            }
        }
    }

    private static boolean isInViewRange(WorldPoint wp1, WorldPoint wp2)
    {
        int distance = wp1.distanceTo(wp2);
        return distance < MAX_ACTOR_VIEW_RANGE;
    }

    private static WorldPoint getWorldLocationBehind(NPC npc)
    {
        final int orientation = npc.getOrientation() / 256;
        int dx = 0, dy = 0;

        switch (orientation)
        {
            case 0: // South
                dy = -1;
                break;
            case 1: // Southwest
                dx = -1;
                dy = -1;
                break;
            case 2: // West
                dx = -1;
                break;
            case 3: // Northwest
                dx = -1;
                dy = 1;
                break;
            case 4: // North
                dy = 1;
                break;
            case 5: // Northeast
                dx = 1;
                dy = 1;
                break;
            case 6: // East
                dx = 1;
                break;
            case 7: // Southeast
                dx = 1;
                dy = -1;
                break;
        }

        final WorldPoint currWP = npc.getWorldLocation();
        return new WorldPoint(currWP.getX() - dx, currWP.getY() - dy, currWP.getPlane());
    }

    private void memorizeNpc(NPC npc)
    {
        final int npcIndex = npc.getIndex();
        memorizedNpc = new MemorizedNpc(npc);
    }

    private void validateSpawnedNpcs()
    {
        if (skipNextSpawnCheck)
        {
            skipNextSpawnCheck = false;
        }
        else
        {
            for (NPC npc : despawnedNpcsThisTick)
            {
                if (!teleportGraphicsObjectSpawnedThisTick.isEmpty())
                {
                    if (teleportGraphicsObjectSpawnedThisTick.contains(npc.getWorldLocation()))
                    {
                        // NPC teleported away, so we don't want to add the respawn timer
                        continue;
                    }
                }

                if (isInViewRange(client.getLocalPlayer().getWorldLocation(), npc.getWorldLocation()))
                {
                    if (memorizedNpc.getNpcIndex() == npc.getIndex())
                    {
                        memorizedNpc.setDiedOnTick(client.getTickCount() + 1); // This runs before tickCounter updates, so we add 1
                    }
                }
            }

            for (NPC npc : spawnedNpcsThisTick)
            {
                if (!teleportGraphicsObjectSpawnedThisTick.isEmpty())
                {
                    if (teleportGraphicsObjectSpawnedThisTick.contains(npc.getWorldLocation()) ||
                            teleportGraphicsObjectSpawnedThisTick.contains(getWorldLocationBehind(npc)))
                    {
                        // NPC teleported here, so we don't want to update the respawn timer
                        continue;
                    }
                }

                if (lastPlayerLocation != null && isInViewRange(lastPlayerLocation, npc.getWorldLocation()))
                {
                    if (memorizedNpc != null && memorizedNpc.getNpcIndex() == npc.getIndex())
                    {
                        if (memorizedNpc.getDiedOnTick() != -1)
                        {
                            memorizedNpc.setRespawnTime(client.getTickCount() + 1 - memorizedNpc.getDiedOnTick());
                            memorizedNpc.setDiedOnTick(-1);
                        }

                        final WorldPoint npcLocation = npc.getWorldLocation();

                        // An NPC can move in the same tick as it spawns, so we also have
                        // to consider whatever tile is behind the npc
                        final WorldPoint possibleOtherNpcLocation = getWorldLocationBehind(npc);

                        memorizedNpc.getPossibleRespawnLocations().removeIf(x ->
                                x.distanceTo(npcLocation) != 0 && x.distanceTo(possibleOtherNpcLocation) != 0);

                        if (memorizedNpc.getPossibleRespawnLocations().isEmpty())
                        {
                            memorizedNpc.getPossibleRespawnLocations().add(npcLocation);
                            memorizedNpc.getPossibleRespawnLocations().add(possibleOtherNpcLocation);
                        }
                    }

                }
            }
        }

        spawnedNpcsThisTick.clear();
        despawnedNpcsThisTick.clear();
        teleportGraphicsObjectSpawnedThisTick.clear();
    }
}
