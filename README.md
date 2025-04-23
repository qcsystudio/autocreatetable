# autocratetable

#### 介绍
基于springboot的自动月表创建工具

#### 软件架构
- 基于springboot 使用了JdbcTemplate 需要配置springboot数据源
- master分支：基于springboot最新版本


- 基本原理：
1. 程序找到参考的表。
2. 根据参考表获取创建新表语句
3. 根据参考表获取索引语句
4. 根据参考表获取唯一键语句
5. 根据参考表获取触发器语音
6、创建新表、索引、唯一键、触发器
#### 使用说明
需要账号拥有表管理权限。需要数据库表名忽略大小写

数据库支持：
- mysql
- oracle
- postgresql
- guassdb(未验证)
#### 安装教程

- 单独运行系统，可以直接配合autocreatetable-test单独运行系统,切记删除controller 包，否则会有sql 输入漏洞。
- 集成到原有系统中，可以直接在原有系统中配置集成autocreatetable-core包。

#### 配置
- pom.xml
```xml
<dependency>
    <groupId>io.github.qcsystudio</groupId>
    <artifactId>autocreatetable</artifactId>
    <version>1.4</version>
</dependency>

```
- Appliction.class
```java
@ComponentScan(basePackages = {"io.github.qcsystudio"})
public class Appliction {
}
```

- application.yml
```yaml
createtable:
  enable:
    onstart: false # 启动程序检查并创建月表.默认 false
    month-first-day: false # 是否以每月1号为每月的第一天，默认false
  target:
    # 表名配置，支持变量： ${yyyyMM}、${yyyyMMdd}、${yyMM}、${yyyy}。
    # 默认以月表最大表为参考表。可以通过xxxx_$\{yyyyMM\}:参考表名 来指定参考表。[max]指使用最大月表为参考表。[min]指使用最小月表为参考表。
    tables: xxxx_$\{yyyyMM\}
    # 目标数据库类型，支持 mysql、oracle、postgresql、guassdb 
    # 如果为空的话会使用 spring.jpa.database。如果 spring.jpa.database 和 autocreatetable.dbtype 都为空的话会使用 mysql
  dbtype: 
```
#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request
