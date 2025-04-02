package org.example.entity;

import lombok.Getter;

/**
 * @author Tyzzzero
 */

@Getter
public enum MenuEnum {
    // 务工监测
    LABOR_MONITORING(0),
    // 脱贫户基础信息
    BASE_iNFO(1),
    // 雨露计划
    YULU_PLAN(3);


    private final int menuNo;


    MenuEnum(int menuNo) {
        this.menuNo = menuNo;
    }


    // 根据 menuNo 获取 MenuEnum 实例
    public static MenuEnum fromMenuNo(int menuNo) {
        for (MenuEnum menu : MenuEnum.values()) {
            if (menu.getMenuNo() == menuNo) {
                return menu;
            }
        }
        throw new IllegalArgumentException("Invalid menuNo: " + menuNo);
    }
}
