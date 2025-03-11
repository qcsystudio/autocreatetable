package com.qcsy.autocreatetable.core.service;

import java.util.List;
import java.util.Map;

/**
 * Description:  暂无
 * Copyright: © 2022 CSTC. All rights reserved.
 * Department:交通信息化部
 *
 * @author luoxiaojian
 * @version 1.0 2022/4/2
 */
public interface TableCreateService{

    /**
     * 自动建表
     * @return 建表列表
     */
    List<String> autoCreateTable();
    /**
     * 依据原有表创建月表
     * @param tableName 表名
     * @param tableSuffixs 表名后缀列表
     * @param isUseMaxTable 是否使用最大的月表为基础进行创建表
     */
    List<String> createTable(String tableName, List<String> tableSuffixs, Boolean isUseMaxTable);

    /**
     * 根据年和月表名创建月表。如果已经存在，则会跳过
     * @param tableNames <table:表名，suffix:表后缀>
     * @param year 年
     * @return 创建的月表列表
     */
    List<String> createTableYears(Map<String,Object> tableNames, String year);
}
