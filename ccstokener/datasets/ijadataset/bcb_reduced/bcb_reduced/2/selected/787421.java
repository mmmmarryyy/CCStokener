package net.anydigit.jiliu.protocols.http;

import java.net.InetSocketAddress;
import net.anydigit.jiliu.balance.Balance;
import net.anydigit.jiliu.balance.ServerNode;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 *
 */
public class HttpLoadBalance extends SimpleChannelHandler {

    private Balance balance;

    /**
	 * 
	 */
    public HttpLoadBalance(Balance balance) {
        super();
        this.balance = balance;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!(e.getMessage() instanceof HttpRequest)) {
            throw new Exception("expect HttpRequest but got " + e.getMessage().getClass());
        }
        HttpRequest req = (HttpRequest) e.getMessage();
        HttpProxyRequest request = new HttpProxyRequest(req);
        String remoteAddr = ((InetSocketAddress) e.getRemoteAddress()).getHostName();
        request.setRemoteAddr(remoteAddr);
        ServerNode sn = this.balance.getServer(request);
        assert (sn != null);
        HttpResponse response = new HttpRequestExecutor().execute(sn, req, remoteAddr);
        ChannelFuture f = e.getChannel().write(response);
        f.addListener(ChannelFutureListener.CLOSE);
    }
}
