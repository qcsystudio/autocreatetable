package com.qcsy.autocreatetable.core.utils;

/**
 * Description: database util
 *
 * @author qcsy
 * @version 2025/3/12
 */
public class DbUtil {
    /**
     * safe sql
     * @param sql old sql
     * @return target data
     */
    public static String safeSql(String sql){
        String result=sql.replace("drop table","")
                .replace("DROP TABLE","")
                .replace("delete from","")
                .replace("DELETE FROM","")
                .replace("TRUNCATE ","")
                .replace("truncate ","")
                .replace("update ","")
                .replace("UPDATE ","");
        return result;
    }
}
