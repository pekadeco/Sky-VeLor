/*
 * Adapted from Better NEF v34 by ii8we for Lunaria. See LICENSE and
 * LICENSE for applicable license and attribution.
 */
package com.arena.pekaclient.events;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;

/** Bridges outbound packets into Forge so packet-sensitive solvers can validate clicks. */
public final class PacketHandler extends ChannelDuplexHandler {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet && MinecraftForge.EVENT_BUS.post(new PacketWriteEvent((Packet) msg))) {
            // Complete the promise because this write was intentionally consumed by a solver.
            promise.trySuccess();
            return;
        }
        super.write(ctx, msg, promise);
    }
}
