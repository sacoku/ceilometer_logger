package com.sangha.weather.device;

import com.sangha.weather.device.inf.CeilometerDeviceInterface;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

public class CL31Device implements CeilometerDeviceInterface {
    public static Logger logger = LoggerFactory.getLogger(CL31Device.class);
    private String label = "CL31";
    private final HashMap<String, Integer> SizeMap = new HashMap<String, Integer>() {
        {
            put("11", 3956); put("12", 2031); put("13", 7606); put("14", 3956); put("15", 55);
            put("21", 3993); put("22", 2068); put("23", 7643); put("24", 3993); put("25", 92);
        }
    };

    /**
     *
     */
    @Override
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
    @Override
    public int decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception {
        int dataSize = 0;

        //Header Magic(?) 확인[☎CL]
        if (!(buf.getByte(0) == 0x01 && buf.getByte(1) == 0x43 && buf.getByte(2) == 0x4C)) {
            logger.warn(label + " - 운고운량계 패킷이 아닙니다. ["
                    + String.format("%02x %02x %02x", buf.getByte(0), buf.getByte(1), buf.getByte(2)) + "]");
            return -1;
        }

        byte [] bMessageNum = new byte[2];
        buf.getBytes(7, bMessageNum);

        try {
            dataSize = SizeMap.get(new String(bMessageNum));
        } catch(Exception e) {
            logger.error(label + " - 사이즈를 알 수 없습니다.[" + String.format("0x%02x 0x%02x", bMessageNum[0], bMessageNum[1]), e);
            throw e;
        }

        return dataSize;
    }

    /**
     *
     * @return
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     */
    @Override
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
