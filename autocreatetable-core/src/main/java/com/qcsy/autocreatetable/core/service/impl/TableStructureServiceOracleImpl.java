package com.qcsy.autocreatetable.core.service.impl;

import com.cstc.sonep.commoninfo.bizmodules.tablecreate.dao.DbTableDao;
import com.cstc.sonep.commoninfo.bizmodules.tablecreate.service.TableStructureService;
import com.cstc.sonep.micro.common.helper.DAOHelper;
import com.cstc.sonep.micro.common.util.StringUtil;
import com.cstc.sonep.micro.frame.jpa.repository.NativeSQL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Description: oracle表结构创建服务类
 * Copyright: © 2023 CSTC. All rights reserved.
 * Department:交通信息化部
 *
 * @author luoxiaojian
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-oracle")
@ConditionalOnProperty(value = "lms.tablecreate.auto_enable",matchIfMissing = false)
public class TableStructureServiceOracleImpl implements TableStructureService {
    private final static String LOG_TITLE="【自动创建月表:ORACLE】";
    /**
     * 获取已经存在的表列表
     * @param schema 表空间
     * @param tableName 表名
     * @return 表列表
     */
    @Override
    public List<String> getExistsTables(String schema, String tableName) {
        String sql = DAOHelper.getSQL(DbTableDao.class, "query_tablenames");
        //替换sql式内
        sql = StringUtil.replaceStance(sql, tableName);
        List<String> tableList = NativeSQL.findByNativeSQL(sql, null).stream().map((a) -> {
            return a.get("tableName") + "";
        }).collect(Collectors.toList());
        return tableList;
    }
    /**
     * 获取建表sql
     * @param tableName 表名
     * @param createTableName 创建表的建表语句
     * @return 建表sql
     */
    @Override
    public List<String> getCreateTableSql(String schema, String tableName,String createTableName,String tableSuffix) {
        String createQuerySql = DAOHelper.getSQL(DbTableDao.class, "query_table_createsql");
        createQuerySql = StringUtil.replaceStance(createQuerySql, tableName);
        List<String> createTableSqlList = NativeSQL.findByNativeSQL(createQuerySql, null).stream().map((a) -> {
            return a.get("tableName") + "";
        }).collect(Collectors.toList());
        String createTableSql = createTableSqlList.get(0);
        //建表语句
        if (createTableSql.contains("CONSTRAINT")) {
            createTableSql = createTableSql.substring(0, createTableSql.indexOf("CONSTRAINT"))
                    + createTableSql.substring(createTableSql.indexOf("PRIMARY"));
        }
        List<Map> columCommentList = NativeSQL.findByNativeSQL(StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_comments"), tableName), null);
        List<String> result=new ArrayList<>();
        result.add(createTableSql);
        columCommentList.stream().forEach((Map node)->{
            //增加描述
            String addCommentSql = "COMMENT ON COLUMN %s.%s IS '%s'";
            try {
                String finalCommontSql=safeSql(String.format(addCommentSql, createTableName, node.get("columnName"), node.get("comments")));
                log.info("{}添加字段描述:[{}]",LOG_TITLE,finalCommontSql);
                result.add(finalCommontSql);
            } catch (Exception e) {
                log.error("{}自动创建月表失败！", LOG_TITLE, e);
            }
        });
        return result;
    }
    /**
     * 获取建索引语句列表
     * @param consultTableName 参考表名
     * @return 表名
     */
    @Override
    public List<String> getCreateIndexSqls(String schema,String consultTableName, String createTableName,String tableSuffix) {
        String queryIndexNameSql =StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_indexs"), consultTableName);
        Map<String, String> indexNames = NativeSQL.findByNativeSQL(queryIndexNameSql, null)
                .stream().collect(Collectors.toMap((k) -> {
                    return k.get("indexName") + "";
                }, (v) -> {
                    return v.get("columnName") + "";
                }, (a, b) -> {
                    return b;
                }));
        List<String> tableIndexSql=new ArrayList<>();
        String queryCreateIndexSqlByIndexName = DAOHelper.getSQL(DbTableDao.class, "query_table_indexscreate");
        for (String indexName : indexNames.keySet()) {
            List<String> createIndexSqls = NativeSQL.findByNativeSQL(StringUtil.replaceStance(queryCreateIndexSqlByIndexName, indexName), null).stream().map((a) -> {
                return a.get("indexSql") + "";
            }).collect(Collectors.toList());
            if (createIndexSqls.size() > 0) {
                tableIndexSql.add(createIndexSqls.get(0));
            }
        }
        return tableIndexSql;
    }

    /**
     * 获取触发器建表语句列表
     * @param schema 表空间
     * @param consultTableName 参考表名
     * @param createTableName 创建表名
     * @return
     */
    @Override
    public List<String> getCreateTriggerSqls(String schema,String consultTableName, String createTableName,String tableSuffix) {
        String queryTriggerNameSql = StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_triggers"), consultTableName);

        List<String> triggerNameList = NativeSQL.findByNativeSQL(queryTriggerNameSql, null).stream().map((a) -> {
            return a.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = DAOHelper.getSQL(DbTableDao.class, "query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = NativeSQL.findByNativeSQL(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName), null).stream().map((a) -> {
                return a.get("a") + "";
            }).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("CREATE ");
            for (String createTriggerSqlRow : createTriggerSqlRowList) {
                stringBuilder.append(createTriggerSqlRow);
            }
            String triggerSql=stringBuilder.toString().
                    replace(triggerName, triggerName + createTableName.substring(createTableName.lastIndexOf("_"))).
                    replace(consultTableName.toUpperCase(), createTableName.toUpperCase());
            createTriggerSqlList.add(safeSql(triggerSql));
        }
        return createTriggerSqlList;
    }
    /**
     * 查询唯一键，主键sql
     * @param schema 表空间
     * @param consultTableName 参考表名
     * @param createTableName 创建表名
     * @return 建表语句
     */
    @Override
    public List<String> getCreateUniqueSqls(String schema,String consultTableName, String createTableName,String tableSuffix) {
        String queryConstraintNameSql = StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_constraint"), consultTableName);
        Map<String, String> constraintNames = NativeSQL.findByNativeSQL(queryConstraintNameSql, null)
                .stream().collect(Collectors.toMap((k) -> {
                    return k.get("constraintName") + "";
                }, (v) -> {
                    return v.get("columnName") + "";
                }, (a, b) -> {
                    return b;
                }));
        String queryCreateConstraintSqlByName = DAOHelper.getSQL(DbTableDao.class, "query_table_constraintcreate");
        List<String> result=new ArrayList<>();
        for (String name : constraintNames.keySet()) {
            List<String> createIndexSqls = NativeSQL.findByNativeSQL(StringUtil.replaceStance(queryCreateConstraintSqlByName, name), null).stream().map((a) -> {
                return a.get("indexSql") + "";
            }).collect(Collectors.toList());
            AtomicInteger index= new AtomicInteger();
            if (createIndexSqls.size() > 0) {
                String createIndexSql=createIndexSqls.get(0);
                String newName= abbreviationIndexName(createTableName)+"_CONS_" + constraintNames.get(name)+"_"+ index.getAndIncrement();
                String createConstanceSql=createIndexSql.
                        replace(name, newName).
                        replace(consultTableName, createTableName.toUpperCase());
                //替换创建索引的语句
                createConstanceSql=createConstanceSql.replaceAll("(USING INDEX )([\\s\\S]*?ENABLE,{0,1})","ENABLE");
                String finalSql=safeSql(createConstanceSql);
                result.add(finalSql);
            }
        }
        return result;
    }
    /**
     * 替换sql里面的 删除、更新语句，避免未知的修改原有表结构的风险
     * @param sql 原有sql
     * @return 目标数据
     */
    private String safeSql(String sql){
        String result=sql.replace("drop table","")
                .replace("DROP TABLE","")
                .replace("delete from","")
                .replace("DELETE FROM","")
                .replace("TRUNCATE ","")
                .replace("truncate ","")
                .replace("update ","")
                .replace("UPDATE ","");
        return result;
    }

    /**
     * 获取入口标记名
     *
     * @param indexName
     * @return
     */
    public String abbreviationIndexName(String indexName) {
        String[] word = indexName.split("_");
        StringBuilder resultWord = new StringBuilder();
        if (word.length > 1) {
            for (int i = 0; i < word.length; i++) {
                if(i==word.length-1){
                    resultWord.append(word[i]);
                }else{
                    resultWord.append(word[i].substring(0, 1));
                }

            }
        } else {
            resultWord.append(indexName);
        }
        return resultWord.toString().toUpperCase();
    }
}
