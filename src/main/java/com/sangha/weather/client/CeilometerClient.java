package com.sangha.weather.client;

import com.sangha.weather.config.ConfigLoader;
import com.sangha.weather.config.Device;
import com.sangha.weather.device.inf.CeilometerDeviceInterface;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CeilometerClient extends Thread {
    public static Logger logger = LoggerFactory.getLogger(CeilometerClient.class);
    Device deviceConfig;
    private int threshold = 0;
    private ChannelFuture channelFuture = null;
    private CeilometerDeviceInterface device = null;
    private long packetCount = 0;
    private boolean isActive = false;
    private EventLoopGroup eventLoop;

    /**
     *
     * @param deviceConfig
     * @throws Exception
     */
    public CeilometerClient(Device deviceConfig, EventLoopGroup eventLoop) throws Exception {
        this.deviceConfig = deviceConfig;
        this.eventLoop = eventLoop;
        setDevice(deviceConfig.getLabel(), CeilometerClientDeviceFactory.createClient(deviceConfig.getType()));
    }

    /**
     *
     * @throws Exception
     */
    public void startClient(EventLoopGroup eventLoop) throws Exception {
        createBootstrap(new Bootstrap(), eventLoop);
    }

    public void stopClient() throws Exception {
        isActive = false;
        if(device != null) device.deinit();
        //if(eventLoop != null) eventLoop.shutdownGracefully().sync();
        channelFuture.channel().close().sync();
        logger.info(device.getLabel() + " - 접속을 종료 했습니다.");
    }

    /**
     *
     * @param bootstrap
     * @param eventLoop
     * @throws Exception
     */
    public void createBootstrap(Bootstrap bootstrap, EventLoopGroup eventLoop) throws Exception {
        try {
            if(device == null)
                throw new NullPointerException("운고운량장치가 등록되어 있지 않습니다.");

            bootstrap.group(eventLoop);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.remoteAddress(new InetSocketAddress(deviceConfig.getIp(), deviceConfig.getPort()));
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    //socketChannel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(1000));
                    socketChannel.pipeline()
                            .addLast("decoder", new ByteToMessageDecoder() {
                                protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                    FileOutputStream output = null;
                                    String outputFileName = null;

                                    if (in.readableBytes() < 16) {
                                        return;
                                    }

                                    in.markReaderIndex();

                                    if(threshold <= 0) threshold = device.decode(ctx, in);
                                    if(threshold == -1) {
                                        in.clear();
                                        return;
                                    }

                                    if (threshold > 0 && in.readableBytes() < threshold)  {
                                        //logger.info("data " + in.readableBytes());
                                        in.resetReaderIndex();
                                        return;
                                    }

                                    logger.info(device.getLabel() + " - [" + packetCount++ + "] 수신 된 데이터 사이즈 : " + in.readableBytes());
                                    byte [] readBuf = new byte[in.readableBytes()];
                                    in.readBytes(readBuf);
                                    threshold = 0;

                                    try {
                                        String path = deviceConfig.getPath();
                                        if(path == null || path.equals("")) {
                                            path = ConfigLoader.getInstance().getDefaultPath();
                                        }

                                        File f = new File(path);
                                        if(!f.exists()) {
                                            f.mkdirs();
                                        }

                                        outputFileName = path + File.separator +
                                                device.getPrefix() + "_" + device.getLabel() + "_" + (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()) + "." + device.getPostfix();
                                        output = new FileOutputStream(outputFileName);
                                        output.write(readBuf);
                                    } catch(Exception ie) {
                                        logger.error(device.getLabel() + " - 파일 쓰기에 실패했습니다("+ outputFileName +")", ie);
                                    } finally {
                                        output.close();
                                    }

                                    //out.add(in.readBytes(readBuf));
                                }

                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception { }
                            })
                            .addLast("simple", new SimpleChannelInboundHandler<Object>() {
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    device.init(ctx);
                                    super.channelActive(ctx);
                                }

                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                    final EventLoop eventLoop = ctx.channel().eventLoop();

                                    logger.info(device.getLabel() + " - 연결이 해제 되었습니다.");
                                    eventLoop.schedule(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if(isActive) {
                                                    logger.info("[" + device.getLabel() + "] 재접속을 시도합니다.");
                                                    CeilometerClient.this.createBootstrap(new Bootstrap(), eventLoop);
                                                }
                                            } catch(Exception e) {
                                                logger.error(device.getLabel() + " - 연결에 실패했습니다.", e);
                                            }
                                        }
                                    }, 5L, TimeUnit.SECONDS);
                                    super.channelInactive(ctx);
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception { logger.info("read...");}
                            });

                }
            });

            isActive = true;
            channelFuture = bootstrap.connect().addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) {
                        logger.info(device.getLabel() + " - 재접속을 시도합니다.");
                        final EventLoop loop = channelFuture.channel().eventLoop();
                        loop.schedule(new Runnable() {
                            public void run() {
                                try {
                                    if(isActive)
                                        CeilometerClient.this.createBootstrap(new Bootstrap(), loop);
                                } catch(Exception e) {
                                    logger.error(device.getLabel() + " - 연결에 실패했습니다.", e);
                                }
                            }
                        }, 5L, TimeUnit.SECONDS);
                    }
                }
            }); //.sync();
            channelFuture.sync();
            //channelFuture.channel().closeFuture().sync();
        } finally {
            //eventLoop.shutdownGracefully().sync();
        }
    }

    /**
     *
     * @param label
     * @param d
     * @return
     */
    public CeilometerClient setDevice(String label, CeilometerDeviceInterface d) {
        this.device = d;
        this.device.setLabel(label);
        return this;
    }

    /**
     *
     * @return
     */
    public CeilometerDeviceInterface getDevice() { return device; }

    @Override
    public void run() {
        try {
            startClient(this.eventLoop);
        } catch(Exception e) {
            logger.error("Thread Start Error", e);
        }
    }
/*
    public static void main(String[] args) {
        try {
            if(args.length < 3) {
                System.out.println("argements required(ip, port, classname)");
                //return ;
            }
            logger.info("Ceilometer Start.");

            CeilometerClient cc = new CeilometerClient(args[0], Integer.parseInt(args[1]));
            cc.registeCeilmeterDevice((CeilometerDeviceInterface)Class.forName(args[2]).newInstance()).startClient();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
*/
}
