package edu.demidov.netchess.client.model.network.netty;

import edu.demidov.netchess.client.model.Options;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;

public class NettyClient {

    private static final int WORKER_GROUP_THREADS = 1;
    private final static Logger log = LoggerFactory.getLogger(NettyClient.class);
    private static NettyClient instance;
    private final String host;
    private final int port;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    private NettyClient() {
        this.host = Options.SERVER_HOST;
        this.port = Options.SERVER_PORT;
    }

    public static synchronized NettyClient getInstance() {
        if (instance == null) instance = new NettyClient();
        return instance;
    }

    public void run() throws InterruptedException, ConnectException {
        log.debug("run; host={}, port={}", host, port);
        workerGroup = new NioEventLoopGroup(WORKER_GROUP_THREADS);

        try {
            final Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(),
                            new NetChessClientHandler()
                    );
                }
            });

            channelFuture = b.connect(host, port).sync();
            log.info("Client connected to server (host={}, port={})", host, port);
        } catch (final InterruptedException ex) {
            log.error("run; host={}, port={}", host, port, ex);
            // В случае исключения - освобождаем ресурсы и перебрасываем исключение выше
            workerGroup.shutdownGracefully();
            throw ex;
        }
    }

    public void stop() throws InterruptedException {
        log.debug("stop");
        try {
            // Wait until the server socket is closed.
            if (channelFuture != null) channelFuture.channel().closeFuture().sync();
        } finally {
            if (workerGroup != null) workerGroup.shutdownGracefully();
        }
    }

    public boolean sendToServer(final NetworkMessage netMsg) throws InterruptedException {
        log.trace("sendToServer netMsg={}", netMsg);
        if (isActive()) {
            channelFuture.channel().writeAndFlush(netMsg).sync();
            return true;
        }
        return false;
    }

    public boolean isActive() {
        if (channelFuture == null) return false;
        return channelFuture.channel().isActive();
    }

}
