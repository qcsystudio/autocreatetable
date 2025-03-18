package io.github.qcsystudio.autocreatetable.test.controller;

import io.github.qcsystudio.autocreatetable.core.service.TableCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Description:
 *
 * @author qcsy
 * @version 1.0 2022/3/18
 */
@RestController
@RequestMapping("/tablecreate")
@Slf4j
public class TableCreateController {

    @Autowired
    private TableCreateService tableCreateService;
    /**
     * generate table by date
     * @param data
     * @return
     */
    @RequestMapping(value = "/byDate")
    public Map<String,Object> byDate(@RequestBody Map data){
        LocalDateTime startTime = LocalDateTime.parse(data.get("startTime")+"");
        LocalDateTime endTime = LocalDateTime.parse(data.get("endTime")+"");
        Map<String,Object> result=new HashMap<>();
        result.put("code", 200);
        result.put("data",  tableCreateService.createTable(data.get("tableName")+"", startTime,endTime,null));
        return result;
    }

    /**
     * generate table by years
     * @param data
     * @return
     */
    @RequestMapping(value = "/byyears")
    public Map<String,Object> byYears(@RequestBody Map data){
        Map<String,Object> result=new HashMap<>();
        result.put("code", 200);
        result.put("data",  tableCreateService.createTableYears(Arrays.asList(data.get("tableName")+""), data.get("year")+""));
        return result;
    }
}
