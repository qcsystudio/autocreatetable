package com.qcsy.autocreatetable.core.service.impl;

import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.map.MapUtil;
import com.qcsy.autocreatetable.core.constant.CommonConstant;
import com.qcsy.autocreatetable.core.domain.TableInfo;
import com.qcsy.autocreatetable.core.helper.SqlHelper;
import com.qcsy.autocreatetable.core.service.TableStructureService;
import com.qcsy.autocreatetable.core.utils.DbUtil;
import com.qcsy.autocreatetable.core.utils.StringUtil;
import com.qcsy.autocreatetable.core.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: oracle table structure service
 *
 * @author qcsy
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-oracle")
public class TableStructureServiceOracleImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:ORACLE】";
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
        sql = StringUtil.replaceStance(sql, tableName);
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
    public  List<String> getCreateTableSql(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo){
        String createQuerySql =SqlHelper.getSql( "query_table_createsql");
        createQuerySql = StringUtil.replaceStance(createQuerySql, structureTablename);
        List<String> createTableSqlList = jdbcTemplate.queryForList(createQuerySql).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get("tableName") + "";
        }).collect(Collectors.toList());
        String createTableSql = createTableSqlList.get(0);
        createTableSql=createTableSql.replaceAll("(?i)"+structureTablename, createTableName);
        //替换CONSTRAINT
        Pattern constrantPattern = Pattern.compile(" CONSTRAINT ([\\S]*)",Pattern.CASE_INSENSITIVE);
        StringBuffer createTableSqlTemp = new StringBuffer();
        Matcher matcher = constrantPattern.matcher(createTableSql);
        while (matcher.find()) {
            matcher.appendReplacement(createTableSqlTemp,String.format("CONSTRAINT \"CON_%s\"", UuidUtil.getUuid()) );
        }
        matcher.appendTail(createTableSqlTemp);
        createTableSql=createTableSqlTemp.toString();
        List<Map<String, Object>> columCommentList = jdbcTemplate.queryForList(StringUtil.replaceStance(SqlHelper.getSql( "query_table_comments"), structureTablename));
        List<String> result=new ArrayList<>();
        result.add(createTableSql);
        columCommentList.stream().forEach((Map a)->{
            Map node= MapUtil.toCamelCaseMap(a);
            //增加描述
            String addCommentSql = "COMMENT ON COLUMN %s.%s IS '%s'";
            try {
                String finalCommontSql=DbUtil.safeSql(String.format(addCommentSql, createTableName, node.get("columnName"), node.get("comments")));
                log.info("{}添加字段描述:[{}]",LOG_TITLE,finalCommontSql);
                result.add(finalCommontSql);
            } catch (Exception e) {
                log.error("{}自动创建月表失败！", LOG_TITLE, e);
            }
        });
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
    public List<String> getCreateIndexSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo){
        String queryIndexNameSql =StringUtil.replaceStance(SqlHelper.getSql( "query_table_indexs"), structureTablename);
        Map<String, String> indexNames = jdbcTemplate.queryForList(queryIndexNameSql)
                .stream().map((a)->{
                    Map node= MapUtil.toCamelCaseMap(a);
                    return node;
                }).collect(Collectors.toMap((k) -> {
                    return k.get("indexName") + "";
                }, (v) -> {
                    return v.get("columnName") + "";
                }, (a, b) -> {
                    return b;
                }));
        List<String> tableIndexSql=new ArrayList<>();
        String queryCreateIndexSqlByIndexName =SqlHelper.getSql( "query_table_indexscreate");
        Pattern suffixPattern =null;
        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
        }else{
            suffixPattern=Pattern.compile("");
        }
        Pattern finalSuffixPattern = suffixPattern;
        for (String indexName : indexNames.keySet()) {
            List<String> createIndexSqls =jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateIndexSqlByIndexName, indexName)).stream().map((a) -> {
                Map node= MapUtil.toCamelCaseMap(a);
                String indexSql = node.get("indexSql") + "";
                if(finalSuffixPattern.matcher(indexName).find()){
                    String newIndexName=indexName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
                    indexSql=indexSql.replace(indexName,newIndexName);
                }else{
                    indexSql=indexSql.replace(indexName, UUID.randomUUID().toString().replace("-",""));
                }
                indexSql=indexSql.replaceAll("(?i)"+structureTablename, createTableName);
                return indexSql;
            }).collect(Collectors.toList());
            if (createIndexSqls.size() > 0) {
                tableIndexSql.add(createIndexSqls.get(0));
            }
        }
        return tableIndexSql;
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
    public List<String> getCreateTriggerSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String queryTriggerNameSql = StringUtil.replaceStance(SqlHelper.getSql( "query_table_triggers"), structureTablename);

        List<String> triggerNameList = jdbcTemplate.queryForList(queryTriggerNameSql).stream().map((a) -> {
            Map node= MapUtil.toCamelCaseMap(a);
            return node.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = SqlHelper.getSql( "query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName)).stream().map((a) -> {
                Map node= new CaseInsensitiveMap(a);
                return node.get("a") + "";
            }).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("CREATE ");
            for (String createTriggerSqlRow : createTriggerSqlRowList) {
                stringBuilder.append(createTriggerSqlRow);
            }
            Pattern suffixPattern =null;
            if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
                suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
            }else{
                suffixPattern=Pattern.compile("");
            }
            String triggerSql=stringBuilder.toString().replaceAll("(?i)"+structureTablename, createTableName);
            if(suffixPattern.matcher(triggerName).find()){
                String newIndexName=triggerName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
                triggerSql=triggerSql.replace(triggerName,newIndexName);
            }else{
                triggerSql=triggerSql.replace(triggerName, String.format("%s_%s", CommonConstant.TRIGGER_PREFIX,UuidUtil.getUuid() ));
            }
            createTriggerSqlList.add(DbUtil.safeSql(triggerSql));
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
    public List<String> getCreateUniqueSqls(String schema,String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
//        String queryConstraintNameSql = StringUtil.replaceStance(SqlHelper.getSql( "query_table_constraint"), structureTablename);
//        Map<String, String> constraintNames = jdbcTemplate.queryForList(queryConstraintNameSql)
//                .stream().map((a) -> {
//                    return MapUtil.toCamelCaseMap(a);
//                }).collect(Collectors.toMap((k) -> {
//                    return k.get("constraintName") + "";
//                }, (v) -> {
//                    return v.get("columnName") + "";
//                }, (a, b) -> {
//                    return b;
//                }));
//        String queryCreateConstraintSqlByName = SqlHelper.getSql( "query_table_constraintcreate");
//        Pattern suffixPattern =null;
//        if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
//            suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
//        }else{
//            suffixPattern=Pattern.compile("");
//        }
//        List<String> result=new ArrayList<>();
//        for (String name : constraintNames.keySet()) {
//            List<String> createIndexSqls =jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateConstraintSqlByName, name)).stream().map((a) -> {
//                Map node= MapUtil.toCamelCaseMap(a);
//                return node.get("indexSql") + "";
//            }).collect(Collectors.toList());
//            if (createIndexSqls.size() > 0) {
//                String createUniqueSql=createIndexSqls.get(0);
//                if(suffixPattern.matcher(name).find()){
//                    String newIndexName=name.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
//                    createUniqueSql=createUniqueSql.replace(name,newIndexName);
//                }else{
//                    createUniqueSql=createUniqueSql.replace(name, UUID.randomUUID().toString().replace("-",""));
//                }
//                if(createUniqueSql.toUpperCase().contains("PRIMARY KEY")){
//                    continue;
//                }
//                createUniqueSql=createUniqueSql.replaceAll("(?i)"+structureTablename, createTableName);
//                createUniqueSql=createUniqueSql.replaceAll("(USING INDEX )([\\s\\S]*?ENABLE,{0,1})","ENABLE");
//                String finalSql= DbUtil.safeSql(createUniqueSql);
//                result.add(finalSql);
//            }
//        }
//        return result;
        //create table sql already has unique sql
        return new ArrayList<>();
    }
}
