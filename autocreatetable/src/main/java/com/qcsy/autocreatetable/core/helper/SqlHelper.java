package com.qcsy.autocreatetable.core.helper;

import lombok.SneakyThrows;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Description: sql helper
 *
 * @author qcsy
 * @version 2025/3/11
 */
@Component
public class SqlHelper {

    private static String dbtype;

    private static ResourceLoader resourceLoader;

    @Autowired
    public SqlHelper(@Value("${tablecreate.dbtype:mysql}") String  dbtype, ResourceLoader resourceLoader ) {
        SqlHelper.dbtype=dbtype;
        SqlHelper.resourceLoader=resourceLoader;
    }

    /**
     * 获取sql
     * @param sqlType sql类型
     * @return
     */
    public static String getSql(String sqlType){
        return getSql(sqlType,dbtype);
    }

    /**
     * 获取sql
     * @param sqlType sql类型
     * @param dbtype 数据库乐享
     * @return
     */
    public static String getSql(String sqlType,String dbtype){
       return getSql("tablecreate.xml",sqlType,dbtype);
    }

    /**
     * 获取sql
     * @param fileName
     * @param sqlType
     * @param dbtype
     * @return
     */
    @SneakyThrows
    public static String getSql(String fileName, String sqlType, String dbtype){
        String daoXmlClassPath =String.format("classpath:mapper/%s",fileName);
        Document xmlDoc = createDoc(resourceLoader.getResource(daoXmlClassPath).getInputStream());
        List<Node> daoList = xmlDoc.selectNodes("/mapper/sql");
        if (null!=daoList&& daoList.size() > 0) {
            for(int i = 0; i < daoList.size(); ++i) {
                Element element = (Element)daoList.get(i);
                String sqlId = element.attribute("id").getValue();
                if(sqlId.equals(sqlType)){
                    Element node=element.element(dbtype);
                    if(null!=node){
                       return node.getTextTrim();
                    }else {
                        node=element.element("common");
                        return node.getTextTrim();
                    }
                }
            }
        }
        return null;
    }

    private  static Document createDoc(InputStream inputStream) {
        SAXReader saxReader = new SAXReader();
        Document doc = null;
        try {
            doc = saxReader.read(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return doc;
    }

    public static String getDialectDBType() {
        return dbtype;
    }
}
