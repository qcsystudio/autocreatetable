package io.github.qcsystudio.autocreatetable.core.utils;

import io.github.qcsystudio.autocreatetable.core.domain.TableInfo;
import io.github.qcsystudio.autocreatetable.core.exception.TableCreateException;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Description: tableInfo工具类
 *
 * @author qcsy
 * @version 2025/3/12
 */
public class TableInfoUtil {
    public static final Map<String, String> SUFFIX_PATTERNS = new HashMap<>();
    static {
        SUFFIX_PATTERNS.put("yyyyMMdd", "([1-2][0,9][0-9]{6})");
        SUFFIX_PATTERNS.put("yyyyMM", "([1-2][0,9][0-9]{4})");
        SUFFIX_PATTERNS.put("yyyy", "([1-2][0,9][0-9]{2})");
        SUFFIX_PATTERNS.put("yyMM", "([0-9]{4})");
    }

    /**
     * 根据表名字符串获取表名信息
     * @param tableName 表名信息
     * @return 表名字符串
     */
    public static TableInfo getTableInfoByName(String tableName) {
        String suffixType ="";
        Matcher matcher = StringUtil.STANCE_PATTERN.matcher(tableName);
        if(matcher.find()){
            suffixType=matcher.group(2).trim();
        }else{
            throw new TableCreateException("error table name");
        }
        String suffixPattern = SUFFIX_PATTERNS.get(suffixType);
        ChronoUnit unit=null;
        switch(suffixType) {
            case "yyyyMMdd":
                unit=ChronoUnit.DAYS;
                break;
            case "yyyyMM":
                unit=ChronoUnit.MONTHS;
                break;
            case "yyyy":
                unit=ChronoUnit.YEARS;
                break;
            case "yyMM":
                unit=ChronoUnit.MONTHS;
                break;
        }
        return new TableInfo(tableName,suffixType,suffixPattern,unit);
    }
}
