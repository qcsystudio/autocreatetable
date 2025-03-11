package com.qcsy.autocreatetable.core.controller;

import com.cstc.sonep.commoninfo.bizmodules.tablecreate.service.TableCreateService;
import com.cstc.sonep.micro.common.facade.ResponsePayload;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * Description:  暂无
 * Copyright: © 2020 CSTC. All rights reserved.
 * Company:CSTC
 *
 * @author luoxiaojian
 * @version 1.0 2022/3/18
 */
@RestController
@RequestMapping("/tablecreate")
@Api(value = "表创建接口",tags = {"表创建接口"})
@Slf4j
@ConditionalOnProperty(value = "lms.tablecreate.auto_enable",matchIfMissing = false)
public class TableCreateController {

    @Autowired
    private TableCreateService tableCreateService;


    /**
     * 根据指定的月表后缀名列表创建所有表名
     * @return
     */
    @ApiOperation(value = "创建表", notes = "根据指定的月表后缀名列表创建所有表名")
    @RequestMapping(value = "/bysuffixs")
    public ResponsePayload bySuffixs(@RequestBody Map data){
        return ResponsePayload.success(tableCreateService.createTable(data.get("tableName")+"", (List<String>) data.get("suffixs"),true));
    }
    /**
     * 根据指定年和月表规则创建月表
     * @return
     */
    @ApiOperation(value = "创建表", notes = "根据指定年和月表规则创建月表")
    @RequestMapping(value = "/byyears")
    public ResponsePayload byYears(@RequestBody Map data){
        return ResponsePayload.success(tableCreateService.createTableYears((Map<String,Object>)data.get("tableName"),  data.get("year")+""));
    }
}
