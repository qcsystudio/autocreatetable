package io.github.qcsystudio.autocreatetable.core.service;

import io.github.qcsystudio.autocreatetable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Description:启动时创建月表
 *
 * @author qcsy
 * @version 2023/11/21
 */
@Component
@Slf4j
public class AutoTableCreateService implements CommandLineRunner {
    private static final String LOG_TITLE="【auto create month table】";

    @Value("${tablecreate.enable.onstart:false}")
    private boolean enableOnStart;

    @Value("${tablecreate.enable.month-first-day:false}")
    private boolean enableMonth;

    @Value("${tablecreate.target.tables:}")
    private String targetTableName;

    @Autowired
    private  TableCreateService tableCreateService;
    @Override
    public void run(String... args) throws Exception {
        if(enableOnStart){
            createTables();
        }else{
            log.info("{} closed onstart create table",LOG_TITLE);
        }
    }

    /**
     * auto create month table(every month 1st 00:00:00,auto check create current month and next 3 month table)
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void autoCreateTable(){
        if(enableMonth){
           createTables();
        }else{
            log.info("{} closed auto create table",LOG_TITLE);
        }
    }

    private List<String> createTables(){
        String split=":";
        LocalDate now = LocalDate.now();
        //当月第一天
        LocalDateTime startDate= now.withDayOfMonth(1).atStartOfDay();
        //三月后的最后一天
        LocalDateTime endDate= now.plusMonths(3).withDayOfMonth(1).minusDays(1).atStartOfDay();
        if(StringUtil.isBlank(targetTableName)){
            log.error("{}cannot Find config table",LOG_TITLE);
            return new ArrayList<>();
        }
        targetTableName=targetTableName.replace("\\{","{").replace("\\}", "}");
        List<String> tableNames= Arrays.asList(targetTableName.split(","));
        List<String> result=new ArrayList<>();
        tableNames.forEach((tableName)->{
            String referenceTableName="";
            if(tableName.contains(split)){
                tableName=tableName.split(split)[0];
                referenceTableName=tableName.split(split)[1];
            }
            result.addAll(tableCreateService.createTable(tableName,startDate,endDate,referenceTableName));
        });
        log.info("{} auto create tables:{}",LOG_TITLE, StringUtil.join(result.iterator(),","));
        return result;
    }


}
