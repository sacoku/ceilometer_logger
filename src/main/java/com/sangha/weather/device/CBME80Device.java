package com.sangha.weather.device;

import com.sangha.weather.device.inf.CeilometerDeviceInterface;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBME80Device implements CeilometerDeviceInterface {
    public static Logger logger = LoggerFactory.getLogger(CBME80Device.class);
    private String label = "CBME80";

    /**
     *
     */
    public void init(Object obj) {
        logger.info(label + " - Device Initialized." + "(" + this.getClass().getName() + ")");
    }

    @Override
    public void deinit() {
        logger.info(label + " Device deinit.");
    }

    /**
     *
     * @param channelHandlerContext
     * @param buf
     * @return
     * @throws Exception
     */
    public int decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception {
        int dataSize = 0;

        if (!(buf.getByte(0) == 0x02 && buf.getByte(1) == 0x0d && buf.getByte(2) == 0x0a)) {
            logger.warn(label + " - 운고운량계 패킷이 아닙니다. ["
                    + String.format("%02x %02x %02x", buf.getByte(0), buf.getByte(1), buf.getByte(2)) + "]");
            return -1;
        }

        return 1591;
    }

    /**
     *
     * @return
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getPrefix() {
        return "cloud";
    }

    @Override
    public String getPostfix() {
        return "txt";
    }
}
