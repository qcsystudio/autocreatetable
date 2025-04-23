package io.github.qcsystudio.autocreatetable.core.service.impl;

import io.github.qcsystudio.autocreatetable.core.domain.TableInfo;
import io.github.qcsystudio.autocreatetable.core.exception.TableCreateException;
import io.github.qcsystudio.autocreatetable.core.helper.SqlHelper;
import io.github.qcsystudio.autocreatetable.core.service.TableCreateService;
import io.github.qcsystudio.autocreatetable.core.service.TableStructureService;
import io.github.qcsystudio.autocreatetable.core.utils.DateTimeUtil;
import io.github.qcsystudio.autocreatetable.core.utils.StringUtil;
import io.github.qcsystudio.autocreatetable.core.utils.TableInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * description:  table create service impl
 *
 *
 * @author qcsy
 * date 2020/07/01
 */
@Slf4j
@Service
@Scope("singleton")
public class TableCreateServiceImpl implements TableCreateService, InitializingBean {
    private static final String LOG_TITLE = "【auto create month table】";
    @Autowired
    private JdbcTemplate jdbcTemplate;
    /**
     * db schema
     */
    @Value("${spring.datasource.schema:}")
    private String tableSchema;
    @Resource(name = "table-structure-oracle")
    private TableStructureService oracleService;
    @Resource(name = "table-structure-mysql")
    private TableStructureService mysqlService;
    @Resource(name = "table-structure-postgresql")
    private TableStructureService postgresqlService;
    @Resource(name = "table-structure-guassdb")
    private TableStructureService guassdbService;

    @Autowired
    private SqlHelper sqlHelper;

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
        List<String> existsTables = getTableStructureService().getExistsTables(this.tableSchema,tableInfo);
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
     * @param tableSuffix table suffix
     * @param tableInfo table info
     * @return table name
     */
    @Transactional
    @Override
    public String createTableByReferenceTable(String referenceTableName, String targetTableName,String tableSuffix, TableInfo tableInfo) {
        List<String> allSql=new ArrayList<>();
        allSql.addAll(getTableStructureService().getCreateTableSql(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(getTableStructureService().getCreateIndexSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(getTableStructureService().getCreateTriggerSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.addAll(getTableStructureService().getCreateUniqueSqls(this.tableSchema,referenceTableName,targetTableName,tableSuffix,tableInfo));
        allSql.forEach((String sql)->{jdbcTemplate.execute(sql);});
        return targetTableName;
    }

    private TableStructureService getTableStructureService() {
        String dbType= sqlHelper.getDialectDBType();
        if("oracle".equalsIgnoreCase(dbType)){
            return oracleService;
        }else if("postgresql".equalsIgnoreCase(dbType)){
            return postgresqlService;
        }else if("guassdb".equalsIgnoreCase(dbType)){
            return guassdbService;
        }else{
            return mysqlService;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try{
            if(StringUtil.isBlank(tableSchema)){
                String dbType= sqlHelper.getDialectDBType();
                if("postgresql".equalsIgnoreCase(dbType)||"guassdb".equalsIgnoreCase(dbType)){
                    this.tableSchema="public";
                }else{
                    this.tableSchema=jdbcTemplate.getDataSource().getConnection().getCatalog();;
                }
            }
        }catch (Exception e){
            log.error("{}get db schema fail！", LOG_TITLE, e);
        }

    }
}
