/*
 * Copyright © 2020 by Sk1er LLC
 *
 * All rights reserved.
 *
 * Sk1er LLC
 * 444 S Fulton Ave
 * Mount Vernon, NY
 * sk1er.club
 */

package club.sk1er.patcher.screen.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class TitleFix {

    private final GuiIngame gui = Minecraft.getMinecraft().ingameGUI;

    @SubscribeEvent
    public void disconnectEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // these are never cleared when logging out of a server while displaying a title or subtitle,
        // so clear these if they're not already cleared when leaving the server.
        if (!this.gui.displayedTitle.isEmpty()) {
            this.gui.displayedTitle = null;
        }

        if (!this.gui.displayedSubTitle.isEmpty()) {
            this.gui.displayedSubTitle = null;
        }
    }
}
