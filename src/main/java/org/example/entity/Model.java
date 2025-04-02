package org.example.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author Tyzzzero
 */
/*
  Excel实体类映射
 */
@Data
public class Model {
    @ExcelProperty
    private long id;
    @ExcelProperty
    private String name;
    @ExcelProperty
    private String idCard;
    @ExcelProperty
    private int subsidy;
    @ExcelProperty
    private String startDate;
    @ExcelProperty
    private String endDate;
    @ExcelProperty
    private String cellPhone;
    @ExcelProperty
    private String province;
    @ExcelProperty
    private String city;
    @ExcelProperty
    private String county;
    @ExcelProperty
    private boolean isWorkingAbroad;
    @ExcelProperty
    private String employmentForm;
    @ExcelProperty
    private String channel1;
    @ExcelProperty
    private String channel2;
    @ExcelProperty
    private String channel3;
    @ExcelProperty
    private int status;
}
