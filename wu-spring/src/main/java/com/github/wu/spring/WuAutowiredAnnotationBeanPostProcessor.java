package com.github.wu.spring;

import com.github.wu.core.rpc.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangyongxu
 */
public class WuAutowiredAnnotationBeanPostProcessor implements ApplicationListener<ApplicationEvent>, BeanPostProcessor, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(WuAutowiredAnnotationBeanPostProcessor.class);

    private WuConfigurationProperties wuConfigurationProperties;

    private ApplicationContext applicationContext;

    private final Map<Class<?>, ReferenceConfig<?>> cachedReference = new HashMap<>();

    public WuAutowiredAnnotationBeanPostProcessor(WuConfigurationProperties wuConfigurationProperties) {
        this.wuConfigurationProperties = wuConfigurationProperties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithLocalFields(bean.getClass(), field -> {
            WuInject fieldAnnotation = field.getAnnotation(WuInject.class);
            if (fieldAnnotation != null) {
                Object value;
                try {
                    value = applicationContext.getBean(field.getType());
                } catch (NoSuchBeanDefinitionException e) {
                    value = getOrCreateRef(field.getType());
                }
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, bean, value);
            }
        });
        ReflectionUtils.doWithLocalMethods(bean.getClass(), method -> {
            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
            if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }
            WuInject methodAnnotation = findAnnotation(method);
            if (methodAnnotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, bean.getClass()))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    logger.info("WuInject annotation is not supported on static methods: {}", method);
                }
                if (method.getParameterCount() == 0) {
                    logger.info("WuInject annotation should only be used on methods : {} with parameters", method);
                }
                Parameter[] parameters = method.getParameters();
                Object[] arguments = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    Object value;
                    /*try {
                        value = applicationContext.getBean(parameters[i].getType());
                    } catch (NoSuchBeanDefinitionException e) {

                    }*/value = getOrCreateRef(parameters[i].getType());
                    arguments[i] = value;
                }
                ReflectionUtils.makeAccessible(method);
                try {
                    method.invoke(bean, arguments);
                } catch (InvocationTargetException e) {
                    logger.error("invoke wuInject method: {} failed", method, e);
                }
            }

        });
        return bean;
    }

    private WuInject findAnnotation(Method method) {
        WuInject annotation = method.getAnnotation(WuInject.class);
        if (annotation == null) {
            for (Parameter parameter : method.getParameters()) {
                if ((annotation = parameter.getAnnotation(WuInject.class)) != null) {
                    return annotation;
                }
            }
        }
        return null;
    }

    private Object getOrCreateRef(Class<?> interfaceClass) {
        ReferenceConfig<?> referenceConfig = cachedReference.get(interfaceClass);
        if (referenceConfig == null) {
            referenceConfig = new ReferenceConfig<>(interfaceClass, wuConfigurationProperties.getRegistry());
            cachedReference.put(interfaceClass, referenceConfig);
        }
        return referenceConfig.refer();
    }


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            startReference();
        } else if (event instanceof ContextClosedEvent) {
            destroy();
        }
    }

    private void startReference() {
        cachedReference.values().forEach(ReferenceConfig::init);

    }

    private void destroy() {
        cachedReference.values().forEach(ReferenceConfig::destroy);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
