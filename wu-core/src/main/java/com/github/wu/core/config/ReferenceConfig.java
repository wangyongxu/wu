package com.github.wu.core.config;

import com.github.wu.common.LifeCycle;
import com.github.wu.common.URL;
import com.github.wu.core.rpc.Invoker;
import com.github.wu.core.rpc.cluster.FailFastClusterInvoker;
import com.github.wu.core.rpc.proxy.JdkProxyFactory;
import com.github.wu.core.rpc.remoting.filter.FilterRegistry;

/**
 * @author wangyongxu
 */
public class ReferenceConfig<T> implements LifeCycle {


    /**
     * interface class
     */
    private Class<T> interfaceClazz;

    private T ref;

    private final RegistryConfig registryConfig;


    private URL registryURL;

    private Invoker<T> invoker;

    private final JdkProxyFactory proxyFactory = new JdkProxyFactory();

    private FilterRegistry filterRegistry;

//    public ReferenceConfig(Class<T> interfaceClazz, RegistryConfig registryConfig) {
//        this(interfaceClazz, registryConfig, new FilterRegistry());
//    }

    public ReferenceConfig(Class<T> interfaceClazz, RegistryConfig registryConfig, FilterRegistry filterRegistry) {
        this.interfaceClazz = interfaceClazz;
        this.registryConfig = registryConfig;
        this.filterRegistry = filterRegistry;
    }


    public synchronized T refer() {
        if (ref == null) {
            init();
        }
        return ref;
    }


    public void init() {
        initRegistryUrl();
        initRef();
    }

    protected void initRegistryUrl() {
        registryURL = registryConfig.getUrl();
    }

    protected void initRef() {
        if (invoker == null) {
            invoker = new FailFastClusterInvoker<>(registryURL, interfaceClazz);
            invoker.init();
        }
        ref = proxyFactory.getProxy(invoker, new Class[]{interfaceClazz}, filterRegistry);
    }


    @Override
    public void destroy() {
        invoker.destroy();
    }


}
