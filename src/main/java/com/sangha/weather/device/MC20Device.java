package com.sangha.weather.device;

import com.sangha.weather.device.inf.CeilometerDeviceInterface;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MC20Device implements CeilometerDeviceInterface {
    public static Logger logger = LoggerFactory.getLogger(MC20Device.class);
    private String label = "MC20Device";
    private ScheduledExecutorService eExcutor = null;

    @Override
    public void init(Object obj) {
        final ChannelHandlerContext ctx = (ChannelHandlerContext)obj;

        eExcutor = Executors.newSingleThreadScheduledExecutor();
        eExcutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    byte [] sendData = {0x01, 0x03, 0x00, 0x15, 0x00, 0x04, 0x55, (byte)0xCD};
                    ctx.writeAndFlush(Unpooled.buffer(8).writeBytes(sendData));
                    logger.info(label + " - Send Request Data");
                } catch(Exception e1) {
                    logger.error(label + "전송 실패", e1);
                }
            }
        }, 5, 10, TimeUnit.SECONDS);

        logger.info(label + " - Device Initialized." + "(" + this.getClass().getName() + ")");
    }

    @Override
    public void deinit() {
        if(eExcutor != null) eExcutor.shutdown();
        logger.info(label + " Device deinit.");
    }

    @Override
    public int decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception {
        if (!(buf.getByte(0) == 0x01 && buf.getByte(1) == 0x03)) {
            logger.warn(label + " - 직달일사계 패킷이 아닙니다. ["
                    + String.format("%02x %02x %02x", buf.getByte(0), buf.getByte(1), buf.getByte(2)) + "]");
            return -1;
        }

        return (13*6);
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getPrefix() {
        return "dsi";
    }

    @Override
    public String getPostfix() {
        return "bin";
    }
}
