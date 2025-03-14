package com.qcsy.autocreatetable.core.service;

import cn.hutool.core.util.StrUtil;
import com.qcsy.autocreatetable.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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

    @Value("${tablecreate.onstart.enable:false}")
    private boolean enableOnStart;

    @Value("${tablecreate.month.enable:false}")
    private boolean enableMonth;

    @Value("${tablecreate.target.tables:}")
    private String targetTableName;

    @Value("${tablecreate.target.reference:}")
    private String referenceTableName;

    @Autowired
    private  TableCreateService tableCreateService;
    @Override
    public void run(String... args) throws Exception {
        if(enableOnStart){
            autoCreateTable();
        }
    }

    /**
     * auto create month table(every month 1st 00:00:00,auto check create current month and next 3 month table)
     * @return List<String>
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public List<String> autoCreateTable(){
        if(enableMonth){
            LocalDate now = LocalDate.now();
            //当月第一天
            LocalDateTime startDate= now.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            //三月后的最后一天
            LocalDateTime endDate= now.plusMonths(4).withDayOfMonth(1).minusDays(1).atStartOfDay();
            if(StringUtil.isBlank(targetTableName)){
                log.error("{}cannot Find config table",LOG_TITLE);
                return new ArrayList<>();
            }
            List<String> tableNames= StrUtil.split(targetTableName,',');
            List<String> result=new ArrayList<>();
            tableNames.forEach((tableName)->{
                result.addAll(tableCreateService.createTable(tableName,startDate,endDate,referenceTableName));
            });
            log.info("{} auto create tables:{}",LOG_TITLE, StrUtil.join(",",result));
            return result;
        }else{
            log.error("{}cannot Find config table",LOG_TITLE);
            return null;
        }
    }


}
