package com.qcsy.autocreatetable.core.utils;

/**
 * Description:
 *
 * @author qcsy
 * @version 2025/3/13
 */
public class UuidUtil {
    /**
     * generate short uuid
     * @return
     */
    public static String getUuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
