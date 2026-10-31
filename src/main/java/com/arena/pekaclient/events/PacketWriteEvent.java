/*
 * Adapted from Better NEF v34 by ii8we for Lunaria. See LICENSE and
 * LICENSE for applicable license and attribution.
 */
package com.arena.pekaclient.events;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Posted immediately before an outgoing Minecraft packet is written to the connection. */
@Cancelable
public final class PacketWriteEvent extends Event {
    public final Packet packet;

    public PacketWriteEvent(Packet packet) {
        this.packet = packet;
    }
}
