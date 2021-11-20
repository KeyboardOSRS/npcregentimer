package net.runelite.client.plugins.npcregentimer;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import javax.inject.Inject;
import java.awt.*;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

class NpcRegenTimerOverlay extends Overlay
{
    private final NpcRegenTimerPlugin npcRegenTimerPlugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    private NpcRegenTimerOverlay(NpcRegenTimerPlugin npcRegenTimerPlugin)
    {
        super(npcRegenTimerPlugin);
        this.npcRegenTimerPlugin = npcRegenTimerPlugin;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.HIGH);

        panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
        panelComponent.setGap(new Point(0, 2));
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Npc regen timer overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        String npcName;
        if (npcRegenTimerPlugin.getMemorizedNpc() != null) {
            npcName = npcRegenTimerPlugin.getMemorizedNpc().getNpcName();
        } else {
            npcName = "None";
        }

        int ticksToRegen = npcRegenTimerPlugin.getTickCountdown();


        final FontMetrics fontMetrics = graphics.getFontMetrics();

        panelComponent.getChildren().clear();

        // Npc name
        int textWidth = Math.max(ComponentConstants.STANDARD_WIDTH, fontMetrics.stringWidth(npcName));
        panelComponent.setPreferredSize(new Dimension(textWidth, 0));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(npcName)
                .build());

        // Ticks to regen
        textWidth = Math.max(textWidth, fontMetrics.stringWidth(String.valueOf(ticksToRegen)));
        panelComponent.setPreferredSize(new Dimension(textWidth, 0));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(String.valueOf(ticksToRegen))
                .build());

        return panelComponent.render(graphics);
    }
}
