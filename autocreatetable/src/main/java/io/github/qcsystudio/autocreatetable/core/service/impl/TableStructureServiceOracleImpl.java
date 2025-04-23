package io.github.qcsystudio.autocreatetable.core.service.impl;

import io.github.qcsystudio.autocreatetable.core.constant.CommonConstant;
import io.github.qcsystudio.autocreatetable.core.domain.TableInfo;
import io.github.qcsystudio.autocreatetable.core.helper.SqlHelper;
import io.github.qcsystudio.autocreatetable.core.service.TableStructureService;
import io.github.qcsystudio.autocreatetable.core.utils.DbUtil;
import io.github.qcsystudio.autocreatetable.core.utils.MapUtil;
import io.github.qcsystudio.autocreatetable.core.utils.StringUtil;
import io.github.qcsystudio.autocreatetable.core.utils.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final static String LOG_TITLE = "【auto create table:ORACLE】";
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SqlHelper sqlHelper;

    /**
     * get exists tables by table info
     *
     * @param schema    database schema
     * @param tableInfo tableInfo
     * @return exists tables
     */
    @Override
    public List<String> getExistsTables(String schema, TableInfo tableInfo) {
        String sql = sqlHelper.getSql(CommonConstant.SQLKEY_QUERY_TABLENAMES);
        String tableName = StringUtil.replaceStance(tableInfo.getTableNameExtension(), "%");
        //替换sql式内
        Pattern pattern = Pattern.compile(String.format("^%s$", StringUtil.replaceStance(tableInfo.getTableNameExtension(), tableInfo.getSuffixPattern())));
        List<String> tableList = jdbcTemplate.queryForList(sql,tableName).stream().map((a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
        }).filter((a) -> {
            return pattern.matcher(a).find();
        }).collect(Collectors.toList());
        return tableList;
    }

    /**
     * get create table sql
     *
     * @param schema             db schema
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateTableSql(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        String createQuerySql = sqlHelper.getSql(CommonConstant.SQLKEY_QUERY_TABLE_CREATESQL);
        createQuerySql = StringUtil.replaceStance(createQuerySql, structureTablename);
        List<String> createTableSqlList = jdbcTemplate.queryForList(createQuerySql).stream().map((a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            return node.get(CommonConstant.CNAME_TABLE_NAME) + "";
        }).collect(Collectors.toList());
        String createTableSql = createTableSqlList.get(0);
        createTableSql = createTableSql.replaceAll("(?i)" + structureTablename, createTableName);
        //replace CONSTRAINT
        Pattern constrantPattern = Pattern.compile(" CONSTRAINT ([\\S]*)", Pattern.CASE_INSENSITIVE);
        StringBuffer createTableSqlTemp = new StringBuffer();
        Matcher matcher = constrantPattern.matcher(createTableSql);
        while (matcher.find()) {
            matcher.appendReplacement(createTableSqlTemp, String.format("CONSTRAINT \"CON_%s\"", UuidUtil.getUuid()));
        }
        matcher.appendTail(createTableSqlTemp);
        createTableSql = createTableSqlTemp.toString();
        //remove index
        createTableSql = createTableSql.replaceAll("USING INDEX ([^,]*) ENABLE", "");
        List<Map<String, Object>> columCommentList = jdbcTemplate.queryForList(sqlHelper.getSql("query_table_comments"), structureTablename);
        List<String> result = new ArrayList<>();
        result.add(createTableSql);
        columCommentList.stream().forEach((Map a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            //增加描述
            String addCommentSql = "COMMENT ON COLUMN %s.%s IS '%s'";
            try {
                String finalCommontSql = DbUtil.safeSql(String.format(addCommentSql, createTableName, node.get("columnName"), node.get("comments")));
                log.info("{}添加字段描述:[{}]", LOG_TITLE, finalCommontSql);
                result.add(finalCommontSql);
            } catch (Exception e) {
                log.error("{}自动创建月表失败！", LOG_TITLE, e);
            }
        });
        return result;
    }

    /**
     * get create index sql
     *
     * @param schema             db schema
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    @Override
    public List<String> getCreateIndexSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        //query oracle constraint .constraint default has index
        String constraintName = "constraintName";
        String columnName = "columnName";
        String queryConstraintSql = sqlHelper.getSql("query_table_constraint");
        Collection<String> constraintNames = jdbcTemplate.queryForList(queryConstraintSql, structureTablename).stream().map((a) -> {
            return MapUtil.toCamelCaseMap(a);
        }).sorted((a, b) -> {
            if (a.get(constraintName).equals(b.get(constraintName))) {
                return a.get(columnName).toString().compareTo(b.get(columnName).toString());
            } else {
                return a.get(constraintName).toString().compareTo(b.get(constraintName).toString());
            }
        }).collect(Collectors.toMap((Map k) -> {
            return k.get(constraintName);
        }, (Map v) -> {
            return v.get(columnName) + "";
        }, (String a, String b) -> {
            return String.format("%s,%s", a, b);
        })).values();
        String queryIndexNameSql =sqlHelper.getSql("query_table_indexs");
        String indexNameKey = "indexName";
        Map<String, String> tempIndexNames = jdbcTemplate.queryForList(queryIndexNameSql, structureTablename).stream()
                .map((a) -> {
                    return MapUtil.toCamelCaseMap(a);
                })
                .sorted((a, b) -> {
                    if (a.get(indexNameKey).equals(b.get(indexNameKey))) {
                        return a.get(columnName).toString().compareTo(b.get(columnName).toString());
                    } else {
                        return a.get(indexNameKey).toString().compareTo(b.get(indexNameKey).toString());
                    }
                }).collect(Collectors.toMap((k) -> {
                    return k.get(indexNameKey) + "";
                }, (v) -> {
                    return v.get(columnName) + "";
                }, (a, b) -> {
                    return String.format("%s,%s", a, b);
                }));
        Map<String, String> indexNames = new HashMap<>();
        tempIndexNames.forEach((k, v) -> {
            if (!constraintNames.contains(v)) {
                indexNames.put(k, v);
            }
        });
        List<String> tableIndexSql = new ArrayList<>();
        String queryCreateIndexSqlByIndexName = sqlHelper.getSql("query_table_indexscreate");
        Pattern suffixPattern = null;
        if (StringUtil.isNotBlank(tableSuffix) && null != tableInfo) {
            suffixPattern = Pattern.compile(tableInfo.getSuffixPattern());
        } else {
            suffixPattern = Pattern.compile("");
        }
        Pattern finalSuffixPattern = suffixPattern;
        indexNames.forEach ((indexName,columns)->{
            String newIndexName="";
            if (finalSuffixPattern.matcher(indexName).find()) {
                newIndexName = indexName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);

            } else {
                newIndexName =String.format("%s_%s", CommonConstant.PREFIX_INDEX, UuidUtil.getUuid());
            }
            tableIndexSql.add(StringUtil.replaceStance(queryCreateIndexSqlByIndexName,newIndexName,createTableName, columns));
        });
        return tableIndexSql;
    }

    /**
     * get create trigger sql
     *
     * @param schema             db schema
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    public List<String> getCreateTriggerSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
        String queryTriggerNameSql =sqlHelper.getSql("query_table_triggers");
        List<String> triggerNameList = jdbcTemplate.queryForList(queryTriggerNameSql,structureTablename).stream().map((a) -> {
            Map node = MapUtil.toCamelCaseMap(a);
            return node.get("triggerName") + "";
        }).collect(Collectors.toList());
        List<String> createTriggerSqlList = new ArrayList<>();
        String queryCreateTriggerSqlByTriggerName = sqlHelper.getSql("query_table_triggercreate");
        for (String triggerName : triggerNameList) {
            List<String> createTriggerSqlRowList = jdbcTemplate.queryForList(queryCreateTriggerSqlByTriggerName, triggerName).stream().map((a) -> {
                Map node = MapUtil.toCamelCaseMap(a);
                return node.get("a") + "";
            }).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("CREATE ");
            for (String createTriggerSqlRow : createTriggerSqlRowList) {
                stringBuilder.append(createTriggerSqlRow);
            }
            Pattern suffixPattern = null;
            if (StringUtil.isNotBlank(tableSuffix) && null != tableInfo) {
                suffixPattern = Pattern.compile(tableInfo.getSuffixPattern());
            } else {
                suffixPattern = Pattern.compile("");
            }
            String triggerSql = stringBuilder.toString().replaceAll("(?i)" + structureTablename, createTableName);
            if (suffixPattern.matcher(triggerName).find()) {
                String newIndexName = triggerName.replaceAll(tableInfo.getSuffixPattern(), tableSuffix);
                triggerSql = triggerSql.replace(triggerName, newIndexName);
            } else {
                triggerSql = triggerSql.replace(triggerName, String.format("%s_%s", CommonConstant.PREFIX_TRIGGER, UuidUtil.getUuid()));
            }
            createTriggerSqlList.add(DbUtil.safeSql(triggerSql));
        }
        return createTriggerSqlList;
    }

    /**
     * get create unique sql
     *
     * @param schema             db schema
     * @param structureTablename structure table name
     * @param createTableName    target table name
     * @param tableSuffix        table suffix
     * @param tableInfo          tableInfo
     * @return result
     */
    public List<String> getCreateUniqueSqls(String schema, String structureTablename, String createTableName, String tableSuffix, TableInfo tableInfo) {
//        String queryConstraintNameSql = StringUtil.replaceStance(sqlHelper.getSql( "query_table_constraint"), structureTablename);
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
//        String queryCreateConstraintSqlByName = sqlHelper.getSql( "query_table_constraintcreate");
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
