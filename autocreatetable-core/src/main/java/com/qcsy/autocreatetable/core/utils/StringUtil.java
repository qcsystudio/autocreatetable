package com.qcsy.autocreatetable.core.utils;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description:
 *
 * @author luoxiaojian
 * @version 2025/3/12
 */
public class StringUtil extends StrUtil {
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

}
