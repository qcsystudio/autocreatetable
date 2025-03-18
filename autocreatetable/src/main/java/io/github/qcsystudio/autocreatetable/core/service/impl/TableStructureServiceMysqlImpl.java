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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Description: about mysql table structure service
 *
 * @author qcsy
 * @version 2023/11/6
 */
@Slf4j
@Service("table-structure-mysql")
public class TableStructureServiceMysqlImpl implements TableStructureService {
    private final static String LOG_TITLE="【auto create table:MYSQL】";
    Pattern INDEX_PATTERN = Pattern.compile("KEY `(.*)` \\(.*\\) USING ([0-9A-Za-z ]*)");
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
        String sql = SqlHelper.getSql(CommonConstant.SQLKEY_QUERY_TABLENAMES);
        String tableName =StringUtil.replaceStance(tableInfo.getTableNameExtension(),"%").toUpperCase() ;
        //替换sql式内
        sql = StringUtil.replaceStance(sql,schema, tableName);
        Pattern pattern = Pattern.compile(String.format("^%s$",StringUtil.replaceStance(tableInfo.getTableNameExtension(),tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql).stream().map((a) -> {
            Map node=MapUtil.toCamelCaseMap(a);
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
    public List<String> getCreateTableSql(String schema, String structureTablename,String createTableName,String tableSuffix,TableInfo tableInfo) {
        String createQuerySql = SqlHelper.getSql(CommonConstant.SQLKEY_QUERY_TABLE_CREATESQL);
        createQuerySql = StringUtil.replaceStance(createQuerySql, structureTablename);
        Map createTableSqlList = jdbcTemplate.queryForMap(createQuerySql, null);
        String createTableSql = createTableSqlList.get("create table").toString();
        //建表语句
        if (createTableSql.contains("CONSTRAINT")) {
            createTableSql = createTableSql.substring(0, createTableSql.indexOf("CONSTRAINT"))
                    + createTableSql.substring(createTableSql.indexOf("PRIMARY"));
        }
        //替换表名为目标表名
        createTableSql=createTableSql.replaceAll("(?i)"+structureTablename,createTableName);
        Matcher matcher = INDEX_PATTERN.matcher(createTableSql);
        Pattern suffixPattern =null==tableInfo?Pattern.compile(""): Pattern.compile(tableInfo.getSuffixPattern());
        while (matcher.find()){
          String indexName=matcher.group(1);
          if(suffixPattern.matcher(indexName).find()&&StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
              String newIndexName=indexName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
              createTableSql=createTableSql.replace(indexName,newIndexName);
          }else{
              createTableSql=createTableSql.replace(indexName, String.format("%s_%s", CommonConstant.PREFIX_INDEX, UuidUtil.getUuid()));
          }
        }
        List<String> result=new ArrayList<>();
        result.add(DbUtil.safeSql(createTableSql));
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
        //mysql create table already has index sql
        return new ArrayList<>();
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
            Map node=MapUtil.toCamelCaseMap(a);
            return node.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = SqlHelper.getSql("query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = jdbcTemplate.queryForList(StringUtil.replaceStance(queryCreateTriggerSqlByTriggerName, triggerName)).stream().map((a) -> {
                return a.get("sql original statement") + "";
            }).collect(Collectors.toList());
            Pattern suffixPattern =null;
            if(StringUtil.isNotBlank(tableSuffix)&&null!=tableInfo){
                suffixPattern=Pattern.compile(tableInfo.getSuffixPattern());
            }else{
                suffixPattern=Pattern.compile("");
            }
            Pattern finalSuffixPattern = suffixPattern;
            createTriggerSqlList.addAll(createTriggerSqlRowList.stream().map((a)->{
                a=a.replaceAll("(?i)"+structureTablename,createTableName);
                a=a.replaceAll("DEFINER=(\\S*)","");
                if(finalSuffixPattern.matcher(triggerName).find()){
                    String newTriggerName=triggerName.replaceAll(tableInfo.getSuffixPattern(),tableSuffix);
                    return a.replaceAll(triggerName,newTriggerName);
                }else{
                    return a.replaceAll(triggerName, String.format("%s_%s", CommonConstant.PREFIX_TRIGGER, UuidUtil.getUuid()));
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
        return new ArrayList<>();
    }
}
