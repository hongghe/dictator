package com.github.liuyuyu.dictator.spring;

import com.github.liuyuyu.dictator.spring.annotation.AutoRefreshValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/*
 * @author liuyuyu
 */
@Slf4j
@Component
public class DictatorAutoRefresher implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private AutowiredAnnotationBeanPostProcessor beanPostProcessor;

    @Scheduled(fixedRateString = "${dictator.client.refresh.rate:1000}")
    public void refresh() {
        //刷新缓存
        DictatorPropertyManager.refreshCache();
        //触发bean更新
        applicationContext.getBeansWithAnnotation(AutoRefreshValue.class)
                .forEach((key, bean) -> {
                    try {
                        beanPostProcessor.processInjection(bean);
                        log.debug("refresh bean {}", bean);
                    } catch (Throwable e) {
                        log.warn("refresh bean {} fail", bean);
                    }
                });
        log.debug("refresh end.");
    }

    @PostConstruct
    public void init() {
        this.beanPostProcessor = applicationContext.getBean(AutowiredAnnotationBeanPostProcessor.class);
        log.info("refresher loaded.");
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }
}
