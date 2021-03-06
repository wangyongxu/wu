package com.github.wu.core.rpc.proxy;

import com.github.wu.common.exception.RpcException;
import com.github.wu.core.rpc.Invoker;
import com.github.wu.core.rpc.remoting.filter.FilterChain;
import com.github.wu.core.rpc.remoting.filter.FilterRegistry;
import com.github.wu.core.rpc.remoting.filter.WuFilter;
import com.github.wu.core.rpc.remoting.transport.ApiResult;
import com.github.wu.core.rpc.remoting.transport.Invocation;
import com.github.wu.core.rpc.remoting.transport.Invocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author wangyongxu
 */
public class InvokerInvocationHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    private final Invoker<?> invoker;

    private FilterRegistry filterRegistry;

    // TODO: 2021-04-16 08:18:54 过滤器注册器 by wangyongxu

    public InvokerInvocationHandler(Invoker<?> invoker, FilterRegistry filterRegistry) {
        this.invoker = invoker;
        this.filterRegistry = filterRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return invoker.toString();
            } else if ("hashCode".equals(methodName)) {
                return invoker.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return invoker.equals(args[0]);
        }
        Invocation invocation = Invocations.parseInvocation(method, args);
        ApiResult apiResult = call(invocation, invoker);

        // FIXME: 2021-05-10 05:25:04 会抛出 java.lang.reflect.UndeclaredThrowableException 异常，应该统一invoker的实现，不借助jdk的代理 by wangyongxu
        return apiResult.recreate();
    }

    protected ApiResult call(Invocation invocation, Invoker<?> invoker) {
        ApiResult result = ApiResult.empty();
        FilterChain filterChain = getFilterChain();
        try {
            boolean before = filterChain.applyBefore(invocation, result);
            if (!before) {
                return result;
            }
            result = invoker.call(invocation);
            filterChain.applyAfter(invocation, result);
        } catch (RpcException e) {
            result.setThrowable(e);
            filterChain.applyComplete(invocation, result, e);
        }
        return result;
    }

    protected FilterChain getFilterChain() {
        List<WuFilter> interceptors = filterRegistry.getInterceptors();
        WuFilter[] array = interceptors.toArray(new WuFilter[0]);
        return new FilterChain(array);
    }
}
