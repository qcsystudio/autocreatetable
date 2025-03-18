package io.github.qcsystudio.autocreatetable.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.temporal.ChronoUnit;

/**
 * Description: table name about
 *
 * @author qcsy
 * @version 2025/3/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableInfo {
    /**
     * tableName
     */
    private String tableNameExtension;
    /**
     * table suffix type
     */
    private String suffixType;
    /**
     * table suffix pattern
     */
    private String suffixPattern;
    /**
     * table suffix step
     *  6:day equals Calendar.DAY_OF_MONTH
     *  2:month equals Calendar.MONTH
     *  1:year equals Calendar.YEAR
     */
    private ChronoUnit suffixUnit;
}
