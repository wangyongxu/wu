package com.github.wu.core.rpc.remoting;

import com.github.wu.common.URL;
import com.github.wu.common.exception.WuRuntimeException;
import com.github.wu.core.rpc.Invoker;
import com.github.wu.core.rpc.remoting.transport.*;

import java.net.InetSocketAddress;

/**
 * 远程调用器
 * <p>
 * 负责发起远程调用，并获取结果
 *
 * @author wangyongxu
 * @see com.github.wu.core.rpc.remoting.filter.WuFilter
 */
public class RemoteInvoker<T> implements Invoker<T> {

    private final EndPointFactory endPointFactory = new EndPointFactoryImpl();

    private final Object lock = new Object();

    private final URL url;

    private final Class<T> interfaceClass;

    private Client client;


    public RemoteInvoker(URL url, Class<T> interfaceClass) {
        this.url = url;
        this.interfaceClass = interfaceClass;
    }

    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }

    @Override
    public ApiResult call(Invocation invocation) {
        init();
        Request request = new Request(invocation);
        Object body = client.send(request).getBody();
        if (body instanceof ApiResult) {
            return (ApiResult) body;
        }
        throw new WuRuntimeException("not support: " + body.getClass());
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return client.isActive();
    }

    @Override
    public void init() {
        synchronized (lock) {
            if (client == null) {
                client = endPointFactory.createClient(new InetSocketAddress(url.getHost(), url.getPort()));
            }
        }
    }

    @Override
    public void destroy() {
        client.disConnect();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: ").append(url);
        sb.append(", ");
        sb.append(interfaceClass);
        return sb.toString();
    }
}
