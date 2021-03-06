package net.gegy1000.earth.server.message;

import io.netty.buffer.ByteBuf;
import net.gegy1000.earth.TerrariumEarth;
import net.gegy1000.terrarium.Terrarium;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class UpdateDownloadMessage implements IMessage {
    private long count;

    public UpdateDownloadMessage() {
    }

    public UpdateDownloadMessage(long count) {
        this.count = count;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.count = buffer.readVarLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeVarLong(this.count);
    }

    public static class Handler implements IMessageHandler<UpdateDownloadMessage, IMessage> {
        @Override
        public IMessage onMessage(UpdateDownloadMessage message, MessageContext ctx) {
            if (ctx.side.isClient()) {
                Terrarium.PROXY.scheduleTask(ctx, () -> TerrariumEarth.PROXY.updateDownload(message.count));
            }
            return null;
        }
    }
}
