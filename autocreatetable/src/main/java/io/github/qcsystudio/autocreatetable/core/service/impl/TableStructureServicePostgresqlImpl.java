package io.github.qcsystudio.autocreatetable.core.service.impl;

import cn.hutool.core.map.MapUtil;
import io.github.qcsystudio.autocreatetable.core.constant.CommonConstant;
import io.github.qcsystudio.autocreatetable.core.domain.TableInfo;
import io.github.qcsystudio.autocreatetable.core.helper.SqlHelper;
import io.github.qcsystudio.autocreatetable.core.service.TableStructureService;
import io.github.qcsystudio.autocreatetable.core.utils.DbUtil;
import io.github.qcsystudio.autocreatetable.core.utils.StringUtil;
import io.github.qcsystudio.autocreatetable.core.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: table structure service for postgresql
 *
 * @author qcsy
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-postgresql")
public class TableStructureServicePostgresqlImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:POSTGRESQL】";
    private final static String TABLE_SUFFIX_RGX="(_)([1-2][0,9][0-9]{4})";
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SqlHelper sqlHelper;

    /**
     * get exists tables by table info
     * @param schema database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String sql = sqlHelper.getSql(CommonConstant.SQLKEY_QUERY_TABLENAMES);
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%").toUpperCase() ;
        //替换sql式内
        sql = StringUtil.replaceStance(sql,schema, tableName);
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql).stream().map((a) -> {
            Map node=MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
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
        String createQuerySql = sqlHelper.getSql( CommonConstant.SQLKEY_QUERY_TABLE_CREATESQL);
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
        createTableSql=createTableSql.replaceAll("(?i)"+structureTablename,createTableName);
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
        Map<String,Object> params = new HashMap<>();
        params.put("tableSchema", schema);
        params.put("tableName", structureTablename);
        String queryIndexNameSql =StringUtil.replaceStance(sqlHelper.getSql( "query_table_indexs"), params);
        Map<String, String> indexNames = jdbcTemplate.queryForList(queryIndexNameSql)
                .stream().map((a)->{
                    Map node= MapUtil.toCamelCaseMap(a);
                    return node;
                }).collect(Collectors.toMap((k) -> {
                    return k.get(CommonConstant.CNAME_INDEX_NAME) + "";
                }, (v) -> {
                    return v.get(CommonConstant.CNAME_COLUMN_NAME) + "";
                }, (a, b) -> {
                    return b;
                }));
        Pattern suffixPattern =null;
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }else{
            suffixPattern=Pattern.compile("");
        }
        Pattern finalSuffixPattern = suffixPattern;
        List<String> result=new ArrayList<>();
        for (String indexName : indexNames.keySet()) {
            String queryIndexCreateSql =StringUtil.replaceStance(sqlHelper.getSql( "query_table_indexscreate"),schema, structureTablename, indexName);
            result.addAll(jdbcTemplate.queryForList(queryIndexCreateSql).stream().map((Map a)->{
                Map node= MapUtil.toCamelCaseMap(a);
                String indexSql=node.get(CommonConstant.CNAME_INDEX_SQL)+"";
                if(finalSuffixPattern.matcher(indexName).find()){
                    indexSql=indexSql.replace(indexName,indexName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix));
                }else{
                    indexSql=indexSql.replace(indexName, String.format("%s_%s",CommonConstant.PREFIX_INDEX, UuidUtil.getUuid()));
                }
                indexSql=indexSql.replaceAll("(?i)"+structureTablename,createTableName);
                return indexSql;
            }).collect(Collectors.toList()));
        }
        return result;
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
        String queryTriggerNameSql = StringUtil.replaceStance(sqlHelper.getSql("query_table_triggers"),schema, structureTablename);
        List<String> triggerNameList = jdbcTemplate.queryForList(queryTriggerNameSql).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return a.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = sqlHelper.getSql( "query_table_triggercreate");
        Pattern suffixPattern =null;
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }else{
            suffixPattern=Pattern.compile("");
        }
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList =jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName)).stream().map((a) -> {
                return a.get("sql original statement") + "";
            }).collect(Collectors.toList());
            Pattern finalSuffixPattern = suffixPattern;
            createTriggerSqlList.addAll(createTriggerSqlRowList.stream().map((a)->{
                if(finalSuffixPattern.matcher(triggerName).find()){
                    String newTriggerName=triggerName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
                    return a.replaceAll(triggerName,newTriggerName);
                }else{
                    return a.replaceAll(triggerName,String.format("%s_%s",CommonConstant.PREFIX_TRIGGER,UuidUtil.getUuid()));
                }
            }).collect(Collectors.toList()));
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
        // index has already created
        return new ArrayList<>();
    }
}
