package com.github.liuyuyu.dictator.core;

import com.github.liuyuyu.dictator.core.exception.ZKForPathException;
import com.github.liuyuyu.dictator.api.param.ConfigGetParam;
import com.github.liuyuyu.dictator.api.param.ConfigSetParam;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;

import static org.junit.Assert.assertNotNull;

/*
 * @author liuyuyu
 */
@Slf4j
@FixMethodOrder
public class ZookeeperConfigServiceTest {

    private ZookeeperConfigService zookeeperConfigService;

    @Before
    public void setUp() {
        ZkProperties zkProperties = new ZkProperties();
        zkProperties.setZkAddress("localhost:2181");
        zkProperties.setBasePath("/dictator");
        this.zookeeperConfigService = new ZookeeperConfigService(zkProperties);
        this.zookeeperConfigService.init();
    }

    @org.junit.Test(expected = ZKForPathException.class)
    public void set() {
        ConfigSetParam configSetParam = new ConfigSetParam();
        configSetParam.setKey("spring.datasource.url");
        configSetParam.setPath("/app/dev/db");
        configSetParam.setValue("jdbc:mysql://localhost:3306/test?user=root&password=&useUnicode=true&characterEncoding=gbk&autoReconnect=true&failOverReadOnly=false");
        this.zookeeperConfigService.save(configSetParam);
    }

    @org.junit.Test
    public void setIfNotExists() {
        this.set("spring.datasource.url","jdbc:mysql://localhost:3306/test");
        this.set("spring.datasource.username","root");
        this.set("spring.datasource.password","123456");
    }

    private void set(String key,String value){
        ConfigSetParam configSetParam = new ConfigSetParam();
        configSetParam.setKey(key);
        configSetParam.setPath("/app/dev/db");
        configSetParam.setValue(value);
        this.zookeeperConfigService.saveIfNotExists(configSetParam);
    }

    @org.junit.Test
    public void setOrModify() {
        ConfigSetParam configSetParam = new ConfigSetParam();
        configSetParam.setKey("spring.datasource.url");
        configSetParam.setPath("/app/dev/db");
        configSetParam.setValue("jdbc:mysql://localhost:3306/test");
        this.zookeeperConfigService.saveOrModify(configSetParam);
    }

    @org.junit.Test
    public void get() {
        ConfigGetParam configSetParam = new ConfigGetParam();
        configSetParam.setKey("spring.datasource.url");
        configSetParam.setPath("/app/dev/db");
        configSetParam.setDefaultValue("none");
        String value = this.zookeeperConfigService.find(configSetParam);

        log.info("find value :{}", value);
        assertNotNull(value);
    }

}