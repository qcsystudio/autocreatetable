package io.github.qcsystudio.autocreatetable.core.helper;

import io.github.qcsystudio.autocreatetable.core.utils.StringUtil;
import lombok.SneakyThrows;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Description: sql helper
 *
 * @author qcsy
 * @version 2025/3/11
 */
@Service
@Scope("singleton")
public class SqlHelper implements InitializingBean {

    private String dbtype;
    @Autowired
    private ResourceLoader resourceLoader;
    @Value("${tablecreate.dbtype:}")
    private String  createDbType;
    @Value("${spring.jpa.database:mysql}")
    private String  jpaDbType;
    /**
     * get sql
     * @param sqlType sql类型
     * @return sql
     */
    public String getSql(String sqlType){
        return getSql(sqlType,dbtype);
    }

    /**
     * get sql
     * @param sqlType sql类型
     * @param dbtype 数据库乐享
     * @return sql
     */
    public String getSql(String sqlType,String dbtype){
       return getSql("tablecreate.xml",sqlType,dbtype);
    }

    /**
     * get sql
     * @param fileName file name
     * @param sqlType sql type
     * @param dbtype database type
     * @return sql
     */
    @SneakyThrows
    public String getSql(String fileName, String sqlType, String dbtype){
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

    /**
     * get dialect db type
     * @return db type
     */
    public String getDialectDBType() {
        return dbtype;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtil.isNotBlank(createDbType)){
            dbtype=createDbType;
        }else{
            dbtype=jpaDbType;
        }
    }
}
