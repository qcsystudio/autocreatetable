package com.qcsy.autocreatetable.core.service;

import java.util.List;

/**
 * Description:表结构服务
 * Copyright: © 2023 CSTC. All rights reserved.
 * Department:交通信息化部
 *
 * @author luoxiaojian
 * @version 2023/11/6
 */
public interface TableStructureService {

    /**
     * 获取已经存在的表列表
     * @param schema 表空间
     * @param tableName 表名
     * @return 表列表
     */
    List<String> getExistsTables(String schema,String tableName);
    /**
     * 获取建表sql
     * @param consultTableName 参考表名
     * @param createTableName 创建表的建表语句
     * @return 建表sql
     */
    List<String> getCreateTableSql(String schema,String consultTableName,String createTableName,String tableSuffix);

    /**
     * 获取建索引语句列表
     * @param consultTableName 参考表名
     * @param createTableName 创建表名
     * @return 表名
     */
    List<String> getCreateIndexSqls(String schema,String consultTableName,String createTableName,String tableSuffix);

    /**
     * 获取触发器建表语句列表
     * @param schema 表空间
     * @param consultTableName 参考表名
     * @param createTableName 创建表名
     * @return
     */
    List<String> getCreateTriggerSqls(String schema,String consultTableName,String createTableName,String tableSuffix);

    /**
     * 查询唯一键，主键sql
     * @param schema 表空间
     * @param consultTableName 参考表名
     * @param createTableName 创建表名
     * @return 建表语句
     */
    List<String> getCreateUniqueSqls(String schema,String consultTableName,String createTableName,String tableSuffix);
}
