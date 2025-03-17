# autocratetable

#### Description
Automatic monthly table creation tool based on springboot

#### Software Architecture
- Based on Spring Boot, JdbcTemplate is used, and the Spring Boot data source needs to be configured.
- Based on the latest version of Spring Boot.
- master branch: Based on springboot latest version

- Basic principle:
1. The program finds the reference table.
2. Obtain the statement for creating a new table based on the reference table.
3. Obtain the index statement based on the reference table.
4. Obtain the unique key statement based on the reference table.
5. Obtain the trigger statement based on the reference table.
6. Create a new table, index, unique key, and trigger.
#### Instructions
The account must have table management rights. Database table names need to be case-insensitive
Database support:
- mysql
- oracle
- postgresql
- guassdb(not verified)

#### Installation
- Run the system independently. You can directly run the system with autocreatetable-app.
- Integrated into the original system, you can directly configure the autocreatetable-core package on the original system.

#### configuration
- pom.xml
```xml
<dependency>
    <groupId>io.github.qcsystudio</groupId>
    <artifactId>autocreatetable</artifactId>
    <version>1.1</version>
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
    onstart: false # Whether to create a table when the system starts. default false
    month-first-day: false # Whether to create a table on the first day of the month. default false
  target:
    # The table name to be created. can choose the table name with ${yyyyMM}、${yyyyMMdd}、${yyMM}、${yyyy}. 
    # default use max month table name to reference. 
    # you can usexxxx_$\{yyyyMM\}:referenceTableName to set up a reference table name. [max] is use max month table . [min] is use min month table .
    tables: xxxx_$\{yyyyMM\} 
 
```


#### Contribution

1.  Fork the repository
2.  Create Feat_xxx branch
3.  Commit your code
4.  Create Pull Request
