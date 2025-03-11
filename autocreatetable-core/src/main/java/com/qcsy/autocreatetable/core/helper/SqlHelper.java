package com.qcsy.autocreatetable.core.helper;

/**
 * Description: sql helper
 *
 * @author qcsy
 * @version 2025/3/11
 */
public class SqlHelper {
    public String getSql(String sqlType) throws Exception {
        Resource resource = resourceLoader.getResource(sqlFilePath);
        return new String(Files.readAllBytes(Paths.get(resource.getURI())));
    }
}
