package com.qcsy.autocreatetable.core.service.impl;

import com.qcsy.autocreatetable.core.domain.TableInfo;
import com.qcsy.autocreatetable.core.exception.TableCreateException;
import com.qcsy.autocreatetable.core.service.TableCreateService;
import com.qcsy.autocreatetable.core.service.TableStructureService;
import com.qcsy.autocreatetable.core.utils.DateTimeUtil;
import com.qcsy.autocreatetable.core.utils.StringUtil;
import com.qcsy.autocreatetable.core.utils.TableInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @description: table create service impl
 *
 *
 * @author qcsy
 * @date 2020/07/01
 */
@Slf4j
@Service
public class TableCreateServiceImpl implements TableCreateService {
    private static final String LOG_TITLE = "【auto create month table】";

    @Value("${lms.tablecreate.tables:{}}")
    private String tableInfos;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * table structure service
     */
    private TableStructureService tableStructureService;
    /**
     * db schema
     */
    private String tableSchema;

    public TableCreateServiceImpl(@Qualifier("table-structure-oracle") TableStructureService oracleService,
                                  @Qualifier("table-structure-mysql") TableStructureService mysqlService,
                                  @Qualifier("table-structure-postgresql") TableStructureService postgresqlService,
                                  @Qualifier("table-structure-guassdb") TableStructureService guassdbService,
                                  JdbcTemplate jdbcTemplate,
                                  @Value("${tablecreate.dbtype:}") String  dbType,
                                  @Value("${spring.jpa.database:mysql}") String  jpaDbType
                                  ) {
        try{
            String dbSchema=jdbcTemplate.getDataSource().getConnection().getCatalog();
            if(StringUtil.isBlank(dbSchema)){
                this.tableSchema="";
            }else{
                this.tableSchema=jdbcTemplate.getDataSource().getConnection().getCatalog();
            }

        }catch (Exception e){
            log.error("{}get db schema fail！", LOG_TITLE, e);
        }
        if(StringUtil.isBlank(dbType)){
            dbType=jpaDbType;
        }
        if("oracle".equalsIgnoreCase(dbType)){
            this.tableStructureService=oracleService;
        }else if("postgresql".equalsIgnoreCase(dbType)){
            this.tableStructureService=postgresqlService;
            this.tableSchema="public";
        }else if("guassdb".equalsIgnoreCase(dbType)){
            this.tableStructureService=guassdbService;
            this.tableSchema="public";
        }else{
            this.tableStructureService=mysqlService;
        }
    }

    /**
     * reference table to create month table
     * @param tableNameExtension target table name  expression
     * @param startTime start time
     * @param endTime end time
     * @param referenceTableName reference table name. null or [max] means use max table. [min] means use min table.
     */
    @Override
    public List<String> createTable(String tableNameExtension, LocalDateTime startTime, LocalDateTime endTime, String referenceTableName) {
        TableInfo tableInfo= TableInfoUtil.getTableInfoByName(tableNameExtension);
        List<String> existsTables = tableStructureService.getExistsTables(this.tableSchema,tableInfo);
        String structureTablenameTemp = null;
        if (StringUtil.isBlank(referenceTableName)||"[max]".equals(referenceTableName)) {
            if(null==existsTables|| existsTables.isEmpty()){
                throw new TableCreateException("could not find structure table!");
            }
            structureTablenameTemp = existsTables.stream().max(Comparator.comparing((a) -> {
                return a;
            })).get();
        }else if ("[min]".equals(referenceTableName)){
            if(null==existsTables|| existsTables.isEmpty()){
                throw new TableCreateException("could not find structure table!");
            }
            structureTablenameTemp = existsTables.stream().max(Comparator.comparing((a) -> {
                return a;
            })).get();
        }else{
            structureTablenameTemp = referenceTableName;
        }
        final String structureTablename = structureTablenameTemp.toUpperCase();
        List<String> tableSuffixs= DateTimeUtil.getDateStrsBetween(startTime, endTime, tableInfo.getSuffixType(), tableInfo.getSuffixUnit());
        Iterator<String> iterable=tableSuffixs.iterator();
        while (iterable.hasNext()){
            String suffix=iterable.next();
            String targetTableName= StringUtil.replaceStance(tableNameExtension, suffix);
            if(existsTables.contains(targetTableName.toUpperCase()) ||existsTables.contains(targetTableName.toLowerCase())){
                iterable.remove();
            }
        }
        List<String> createTableNames = new ArrayList<String>();
        for (String tableSuffix : tableSuffixs) {
            //target table name
            String createTableName=StringUtil.replaceStance(tableNameExtension,tableSuffix);
            log.info("{}start create table ->target table：{} | structure table：{}",LOG_TITLE,createTableName,structureTablename);
            createTableByReferenceTable(structureTablename,createTableName,tableSuffix,tableInfo);
            createTableNames.add(createTableName);
        }
        return createTableNames;
    }

    /**
     * create table by year
     * @param tableNameStructures target table names
     * @param year year
     * @return results
     */
    @Override
    public List<String> createTableYears(List<String> tableNameStructures, String year) {
        if (null == tableNameStructures || tableNameStructures.size() < 1) {
            throw new TableCreateException("could not find structure table!");
        }
        LocalDateTime startTime = LocalDateTime.of(Integer.parseInt(year), 1, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(Integer.parseInt(year), 12, 31, 23, 59, 59);
        List<String> result=new ArrayList<>();
        tableNameStructures.forEach((tableName) -> {
            //生成月表数据
            result.addAll(createTable(tableName,startTime,endTime,null));
        });
        return result;
    }

    /**
     *
     * create table by reference table
     * @param referenceTableName reference table name
     * @param targetTableName target table name
     * @param tableSuffix
     * @param tableInfo
     * @return
     */
    @Transactional
    @Override
    public String createTableByReferenceTable(String referenceTableName, String targetTableName,String tableSuffix, TableInfo tableInfo) {
        List<String> allSql=new ArrayList<>();
        allSql.addAll(tableStructureService.getCreateTableSql(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(tableStructureService.getCreateIndexSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(tableStructureService.getCreateTriggerSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(tableStructureService.getCreateUniqueSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.forEach((String sql)->{jdbcTemplate.execute(sql);});
        return targetTableName;
    }
}
