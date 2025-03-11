package com.qcsy.autocreatetable.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Description:程序启动就自动创建月表
 * Copyright: © 2023 CSTC. All rights reserved.
 * Department:交通信息化部
 *
 * @author luoxiaojian
 * @version 2023/11/21
 */
@Component
@ConditionalOnProperty(value = "lms.tablecreate.auto_enable",matchIfMissing = false)
public class OnStartTableCreateService  implements CommandLineRunner {
    @Autowired
    private  TableCreateService tableCreateService;
    @Override
    public void run(String... args) throws Exception {
        tableCreateService.autoCreateTable();
    }
}
