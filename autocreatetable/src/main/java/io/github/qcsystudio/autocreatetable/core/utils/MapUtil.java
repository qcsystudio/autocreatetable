package io.github.qcsystudio.autocreatetable.core.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 *
 * @author qcsy
 * @version 2025/4/22
 */
public class MapUtil {
    /**
     * map to camelCase map
     * @param originalMap target map
     * @return camelCase map
     */
    public static Map<String, Object> toCamelCaseMap(Map<String, Object> originalMap) {
        Map<String, Object> camelCaseMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            String camelCaseKey = toCamelCase(key);
            camelCaseMap.put(camelCaseKey, entry.getValue());
        }
        return camelCaseMap;
    }

    /**
     * string to camelCase
     * @param key string key
     * @return camelCase key
     */
    private static String toCamelCase(String key) {
        StringBuilder camelCaseKey = new StringBuilder();
        boolean capitalizeNext = false;
        for (char ch : key.toCharArray()) {
            if (ch == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                camelCaseKey.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                camelCaseKey.append(Character.toLowerCase(ch));
            }
        }
        return camelCaseKey.toString();
    }
}
