package com.github.wu.core.rpc.loadbalance;

import com.github.wu.common.URL;
import com.github.wu.core.rpc.Invoker;
import com.github.wu.core.rpc.remoting.transport.Invocation;

import java.util.List;

/**
 * @author qiankewei
 */
public class WeightedLoadBalance implements LoadBalance {


    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        throw new UnsupportedOperationException("not weighted");
    }

    @Override
    public String toString() {
        return "weight";
    }
}
