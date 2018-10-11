package com.sangha.weather.device.inf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface CeilometerDeviceInterface {
    void init(Object obj);
    void deinit();
    int decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf) throws Exception;
    String getLabel();
    void setLabel(String label);
    String getPrefix();
    String getPostfix();
}
