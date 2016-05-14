package edu.demidov.netchess.server.model.network.netty;

import edu.demidov.netchess.server.model.Options;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer
{
    private final int BOSS_GROUP_THREADS = 1;
    private final int WORKER_GROUP_THREADS = 1;
    private final int CHANNEL_SO_BACKLOG = 128;

    private final String host;
    private final int port;
    
    private static NettyServer instance;
    private final static Logger log = LoggerFactory.getLogger(NettyServer.class);
    
    /* Для небольшого проекта вполне хватит по одному потоку для 'boss' и 'worker' EventLoopGroup.
    - 'boss' - accepts an incoming connection;
    - 'worker' - handles the traffic of the accepted connection once the boss accepts the connection and registers the accepted connection to the worker.
     */
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    
    public static synchronized NettyServer getInstance()
    {
        if (instance == null) instance = new NettyServer();
        return instance;
    }
    
    private NettyServer()
    {
        this.host = Options.SERVER_HOST;
        this.port = Options.SERVER_PORT;
    }

    public void run() throws InterruptedException
    {
        log.debug("run at host={}, port={}", host, port);
        bossGroup = new NioEventLoopGroup(BOSS_GROUP_THREADS);
        workerGroup = new NioEventLoopGroup(WORKER_GROUP_THREADS);
        
        try
        {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception
                    {
                        ch.pipeline().addLast(
                                //new LoggingHandler(LogLevel.INFO),
                                new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                new NetChessServerHandler()
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, CHANNEL_SO_BACKLOG)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            channelFuture = b.bind(host, port).sync();
            log.info("Server started at host={}, port={}", host, port);
        } catch (final InterruptedException ex)
        {
            log.error("run; host={}, port={}", host, port, ex);
            // В случае исключения - освобождаем ресурсы и перебрасываем исключение в функцию выше
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            throw ex;
        }
    }

    public void stop() throws InterruptedException
    {
        log.debug("stop");
        try
        {
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            channelFuture.channel().closeFuture().sync();
        } finally
        {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
