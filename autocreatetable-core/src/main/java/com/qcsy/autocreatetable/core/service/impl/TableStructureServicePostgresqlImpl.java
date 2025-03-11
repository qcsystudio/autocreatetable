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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: Postgresql表结构创建服务类
 * Copyright: © 2023 CSTC. All rights reserved.
 * Department:交通信息化部
 *
 * @author luoxiaojian
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-postgresql")
@ConditionalOnProperty(value = "lms.tablecreate.auto_enable",matchIfMissing = false)
public class TableStructureServicePostgresqlImpl implements TableStructureService {
    private final static String LOG_TITLE="【自动创建月表:POSTGRESQL】";
    private final static String TABLE_SUFFIX_RGX="(_)([1-2][0,9][0-9]{4})";
    private final static Pattern TABLE_SUFFIX_PATTERN=Pattern.compile(TABLE_SUFFIX_RGX);

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
        sql = StringUtil.replaceStance(sql,schema, tableName);
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
        Map createTableSqlList = NativeSQL.findOneByNativeSQL(createQuerySql, null);
        String createTableSql = createTableSqlList.get("create table").toString();
        //建表语句
        if (createTableSql.contains("CONSTRAINT")) {
            createTableSql = createTableSql.substring(0, createTableSql.indexOf("CONSTRAINT"))
                    + createTableSql.substring(createTableSql.indexOf("PRIMARY"));
        }
        //替换表名为目标表名
        createTableSql=createTableSql.toUpperCase().replace(tableName.toUpperCase(),createTableName.toUpperCase());
        createTableSql=createTableSql.replaceAll(TABLE_SUFFIX_RGX,"_"+tableSuffix);
        List<String> result=new ArrayList<>();
        String sql=safeSql(createTableSql);
        sql=sql.replaceAll("(CREATE INDEX)(.*)(\\;)","");
        result.add(sql);
        return result;
    }
    /**
     * 获取建索引语句列表
     * @param consultTableName 参考表名
     * @return 表名
     */
    @Override
    public List<String> getCreateIndexSqls(String schema,String consultTableName, String createTableName,String tableSuffix) {
        String queryIndexNameSql =StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_indexs"),schema, consultTableName);
        return NativeSQL.findByNativeSQL(queryIndexNameSql, null).stream().map((Map node)->{
            String indexName=node.get("indexName")+"";
            //处理月表后缀
            indexName=indexName.replaceAll(TABLE_SUFFIX_RGX,"_"+tableSuffix);
            if(!TABLE_SUFFIX_PATTERN.matcher(indexName).find()){
                indexName=indexName+"_"+tableSuffix;
            }
            String columnName=node.get("columnName")+"";
            return "ALTER TABLE "+createTableName+" ADD INDEX "+indexName+"("+columnName+")";
        }).collect(Collectors.toList());
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
        String queryTriggerNameSql = StringUtil.replaceStance(DAOHelper.getSQL(DbTableDao.class, "query_table_triggers"),schema, consultTableName);
        List<String> triggerNameList = NativeSQL.findByNativeSQL(queryTriggerNameSql, null).stream().map((a) -> {
            return a.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = DAOHelper.getSQL(DbTableDao.class, "query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = NativeSQL.findByNativeSQL(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName), null).stream().map((a) -> {
                return a.get("sql original statement") + "";
            }).collect(Collectors.toList());
            createTriggerSqlList.addAll(createTriggerSqlRowList.stream().map((a)->{return a.replaceAll(TABLE_SUFFIX_RGX,""+tableSuffix);}).collect(Collectors.toList()));
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
        return new ArrayList<>();
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
}
