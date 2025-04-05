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
    /**
     * 行编号
     */
    @ExcelProperty
    private long id;
    /**
     * 姓名
     */
    @ExcelProperty
    private String name;
    /**
     * 证件号码
     */
    @ExcelProperty
    private String idCard;
    /**
     * 补贴金额/收入金额
     */
    @ExcelProperty
    private int amount;
    /**
     * 务工开始年月
     */
    @ExcelProperty
    private String startDate;
    /**
     * 务工结束年月
     */
    @ExcelProperty
    private String endDate;
    /**
     * 联系方式
     */
    @ExcelProperty
    private String cellPhone;
    /**
     * 省份
     */
    @ExcelProperty
    private String province;
    /**
     * 城市
     */
    @ExcelProperty
    private String city;
    /**
     * 区县
     */
    @ExcelProperty
    private String county;
}
