package com.qcsy.autocreatetable.core.service;

import com.qcsy.autocreatetable.core.domain.TableInfo;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Description:  create table service
 *
 * @author qcsy
 * @version 1.0 2022/4/2
 */
public interface TableCreateService{

    /**
     * reference table to create month table
     * @param tableNameExtension target table name  expression
     * @param startTime start time
     * @param endTime end time
     * @param referenceTableName reference table name. null or [max] means use max table. [min] means use min table.
     */
    List<String> createTable(String tableNameExtension, LocalDateTime startTime, LocalDateTime endTime, String referenceTableName);

    /**
     * create table by year
     * @param tableNameStructures target table names
     * @param year year
     * @return results
     */
    List<String> createTableYears(List<String> tableNameStructures, String year);


    /**
     * create table by reference table
     * @param referenceTableName reference table name
     * @param targetTableName target table name
     * @return
     */
    String createTableByReferenceTable(String referenceTableName, String targetTableName,String tableSuffix, TableInfo tableInfo);
}
