package com.qcsy.autocreatetable.core.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author luoxiaojian
 * @description: 数据库自动创建表
 * @copyright cstc.
 * @date 2020/4/8
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "lms.tablecreate.auto_enable",matchIfMissing = false)
public class TableCreateServiceImpl implements TableCreateService {
    private static final String LOG_TITLE = "【自动创建月表】";

    @Value("${lms.tablecreate.tables:{}}")
    private String tableInfos;
    /**
     * 是否启用自动创建月表
     */
    @Value("${lms.tablecreate.auto_enable:false}")
    private Boolean autoEnable;

    /**
     * 表结构服务类
     */
    private TableStructureService tableStructureService;
    /**
     * 表名称空间
     */
    private String tableSchema;

    public TableCreateServiceImpl(@Qualifier("table-structure-oracle") TableStructureService oracleService,
                                  @Qualifier("table-structure-mysql") TableStructureService mysqlService,
                                  @Qualifier("table-structure-postgresql") TableStructureService postgresqlService,
                                  @Qualifier("table-structure-guassdb") TableStructureService guassdbService,
                                  ConfigHelper configHelper,
                                  JdbcTemplate jdbcTemplate) {
        String dbType= configHelper.getDialectDBType();
        try{
            this.tableSchema=jdbcTemplate.getDataSource().getConnection().getCatalog();
        }catch (Exception e){
            log.error("{}获取表空间失败！", LOG_TITLE, e);
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
     * 自动创建月表(每个月1号0点，自动检查创建当前月及后3个月的月表数据)
     * @return
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public List<String> autoCreateTable(){
        if(autoEnable){
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            //次月第一天
            Date startDate=cal.getTime();
            Date endTime=DateUtils.addMonth(startDate,3);
            cal.setTime(endTime);
            cal.roll(Calendar.DAY_OF_MONTH, -1);
            //三月后的最后一天
            Date endDate=cal.getTime();
            List<String> result=createTableDate(null,startDate,endDate);
            log.info("{}自动创建了月表:{}",LOG_TITLE,JSONUtil.format(result));
            return result;
        }else{
            log.info("{}没有配置自动月表生成",LOG_TITLE);
            return null;
        }

    }

    /**
     * 依据原有表创建月表
     *
     * @param tableName     表名
     * @param tableSuffixs  表名后缀列表
     * @param isUseMaxTable 是否使用最大的月表为基础进行创建表
     */
    @Override
    public List<String> createTable(String tableName, List<String> tableSuffixs, Boolean isUseMaxTable) {
        List<String> tableList = tableStructureService.getExistsTables(this.tableSchema,tableName);
        Iterator<String> iterable=tableSuffixs.iterator();
        while (iterable.hasNext()){
            String suffix=iterable.next();
            //表名过滤的时候，无论大小写都移除
            if(tableList.contains(String.format("%s_%s", tableName,suffix).toUpperCase())
             ||tableList.contains(String.format("%s_%s", tableName,suffix ).toLowerCase())){
                iterable.remove();
            }
        }
        if (tableList.size() < 1) {
            throw new RuntimeException("没有找到相关表名！");
        }
        String structureTablenameTemp = tableName;
        if (isUseMaxTable) {
            structureTablenameTemp = tableList.stream().max(Comparator.comparing((a) -> {
                return a;
            })).get();
        }
        final String structureTablename = structureTablenameTemp.toUpperCase();

        List<String> createTableNames = new ArrayList<String>();
        for (String tableSuffix : tableSuffixs) {
            //根据时间生成需要创建的表名
            String createTableName=String.format("%s_%s", tableName.toUpperCase(), tableSuffix);
            //去除实际想建的表
            if(tableList.contains(createTableName)){
                continue;
            }
            log.info("{}开始建表，目标表：{} 参考表：{}",LOG_TITLE,createTableName,structureTablename);
            List<String> allSql=new ArrayList<>();
            //查询建表语句
            allSql.addAll(tableStructureService.getCreateTableSql(this.tableSchema,structureTablename,createTableName,tableSuffix));
            //获取索引创建语句
            allSql.addAll(tableStructureService.getCreateIndexSqls(this.tableSchema,structureTablename,createTableName,tableSuffix));
            //获取触发器创建语句
            allSql.addAll(tableStructureService.getCreateTriggerSqls(this.tableSchema,structureTablename,createTableName,tableSuffix));
            //获取唯一键创建语句
            allSql.addAll(tableStructureService.getCreateUniqueSqls(this.tableSchema,structureTablename,createTableName,tableSuffix));
            allSql.forEach((String sql)->{NativeSQL.execute(sql);});
            createTableNames.add(createTableName);
        }
        return createTableNames;
    }

    /**
     * 根据年和月表名创建月表。如果已经存在，则会跳过
     *
     * @param tableNames <表名:表后缀>
     * @param year       年
     * @return 创建的月表列表
     */
    @Override
    public List<String> createTableYears(Map<String, Object> tableNames, String year) {
        if (null == tableNames || tableNames.size() < 1) {
            tableNames = JSONUtil.parseMap(tableInfos.replace("\\",""));
        }
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, Integer.parseInt(year));
        Date startTime=cal.getTime();
        cal.roll(Calendar.DAY_OF_YEAR, -1);
        Date endTime=cal.getTime();
        return createTableDate(tableNames,startTime,endTime);
    }

    /**
     * 依据时间段创建月表数据
     * @param tableNames <表名:表后缀>
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 表名数据
     */
    public List<String> createTableDate(Map<String, Object> tableNames, Date startTime, Date endTime) {
        List<String> result=new ArrayList<>();
        if (null == tableNames || tableNames.size() < 1) {
            tableNames = JSONUtil.parseMap(tableInfos.replace("\\",""));
        }
        tableNames.forEach((tableName, suffix) -> {
            //生成需要的表名
            Map<String,String> needTables = DateUtils.getDateListBetweenDate(startTime,endTime,suffix.toString(),Calendar.MONTH).stream()
                    .collect(Collectors.toMap((a)->{return String.format("%s_%s", tableName.toUpperCase(), a);},(v)->{return v;},(a,b)->{return b;}));
            //生成月表数据
            result.addAll(createTable(tableName,new ArrayList<>(needTables.values()),true));
        });
        return result;
    }
}
