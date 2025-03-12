package com.qcsy.autocreatetable.core.service.impl;

import cn.hutool.core.map.MapUtil;
import com.qcsy.autocreatetable.core.domain.TableInfo;
import com.qcsy.autocreatetable.core.helper.SqlHelper;
import com.qcsy.autocreatetable.core.service.TableStructureService;
import com.qcsy.autocreatetable.core.utils.DbUtil;
import com.qcsy.autocreatetable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class TableStructureServicePostgresqlImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:POSTGRESQL】";
    private final static String TABLE_SUFFIX_RGX="(_)([1-2][0,9][0-9]{4})";
    private final static Pattern TABLE_SUFFIX_PATTERN=Pattern.compile(TABLE_SUFFIX_RGX);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String sql = SqlHelper.getSql("query_tablenames");
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%") ;
        //替换sql式内
        sql = StringUtil.replaceStance(sql,schema, tableName);
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get("tableName") + "";
        }).filter((a) -> {
            return pattern.matcher(a).find();
        }).collect(Collectors.toList());
        return tableList;
    }
    /**
     *  get create table sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTableSql(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String createQuerySql = SqlHelper.getSql( "query_table_createsql");
        createQuerySql = StringUtil.replaceStance(createQuerySql, structureTablename);
        Map createTableSqlList = jdbcTemplate.queryForMap(createQuerySql, null);
        String createTableSql = createTableSqlList.get("create table").toString();
        //建表语句
        if (createTableSql.contains("CONSTRAINT")) {
            createTableSql = createTableSql.substring(0, createTableSql.indexOf("CONSTRAINT"))
                    + createTableSql.substring(createTableSql.indexOf("PRIMARY"));
        }
        //替换表名为目标表名
        createTableSql=createTableSql.toUpperCase().replace(structureTablename.toUpperCase(),createTableName.toUpperCase());
        createTableSql=createTableSql.replaceAll(TABLE_SUFFIX_RGX,"_"+tableSuffix);
        List<String> result=new ArrayList<>();
        String sql= DbUtil.safeSql(createTableSql);
        sql=sql.replaceAll("(CREATE INDEX)(.*)(\\;)","");
        result.add(sql);
        return result;
    }
    /**
     *  get create index sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateIndexSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String queryIndexNameSql =StringUtil.replaceStance(SqlHelper.getSql( "query_table_indexs"),schema, structureTablename);
        return jdbcTemplate.queryForList(queryIndexNameSql).stream().map((Map a)->{
            Map node= MapUtil.toCamelCaseMap(a);
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
     *  get create trigger sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTriggerSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String queryTriggerNameSql = StringUtil.replaceStance(SqlHelper.getSql("query_table_triggers"),schema, structureTablename);
        List<String> triggerNameList = jdbcTemplate.queryForList(queryTriggerNameSql).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return a.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = SqlHelper.getSql( "query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList =jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName)).stream().map((a) -> {
                return a.get("sql original statement") + "";
            }).collect(Collectors.toList());
            createTriggerSqlList.addAll(createTriggerSqlRowList.stream().map((a)->{return a.replaceAll(TABLE_SUFFIX_RGX,""+tableSuffix);}).collect(Collectors.toList()));
        }
        return createTriggerSqlList;
    }
    /**
     *  get create unique sql
     * @param schema db schema
     * @param structureTablename structure table name
     * @param createTableName target table name
     * @param tableSuffix  table suffix
     * @param tableInfo tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateUniqueSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        return new ArrayList<>();
    }
}
