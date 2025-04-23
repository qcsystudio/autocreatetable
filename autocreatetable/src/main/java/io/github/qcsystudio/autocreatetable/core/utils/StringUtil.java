package io.github.qcsystudio.autocreatetable.core.utils;


import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description:
 *
 * @author qcsy
 * @version 2025/3/12
 */
public class StringUtil{
    private static final int STRING_BUILDER_SIZE = 256;
    public static final String EMPTY = "";
    public static final Pattern STANCE_PATTERN = Pattern.compile("(\\$\\{)([^\\}^\\{]+)(\\})");

    /**
     * 占位符替换 例如 ${xxx}
     * @param target 替换目标
     * @param info 字段映射信息
     * @return 替换完成后的字符串信息
     */
    public static String replaceStance(String target, Map<String,Object> info){
        StringBuffer result=new StringBuffer();
        Matcher matcher = STANCE_PATTERN.matcher(target);
        while (matcher.find()) {
            //正则的第二分组
            String key = matcher.group(2).trim();
            if(info.containsKey(key)){
                matcher.appendReplacement(result,Matcher.quoteReplacement(info.get(key).toString()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 占位符替换 例如${xxx}
     * @param target 替换目标
     * @param info 用来替换的字符串
     * @return 替换后的字符串
     */
    public static String replaceStance(String target, String ... info){
        StringBuffer result=new StringBuffer();
        Matcher matcher = STANCE_PATTERN.matcher(target);
        int i=0;
        while (matcher.find()) {
            //正则的第二分组
            String key = matcher.group(2).trim();
            if(i<info.length){
                matcher.appendReplacement(result,Matcher.quoteReplacement(info[i]));
            }else{
                matcher.appendReplacement(result,key);
            }
            i++;
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * is blank
     * @param str
     * @return
     */
    public static boolean isBlank(Object str){
      return null==str||"".equals(str.toString().trim());
    }

    /**
     * is not blank
     * @param str is not blank
     * @return
     */
    public static boolean isNotBlank(Object str){
        return !isBlank(str);
    }

    public static String join(final Iterator<?> iterator, final String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return EMPTY;
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return Objects.toString(first, "");
        }

        // two or more elements
        final StringBuilder buf = new StringBuilder(STRING_BUILDER_SIZE); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }
}
