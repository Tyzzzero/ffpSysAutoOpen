package org.example.entity;

import lombok.Getter;

/**
 * @author Tyzzzero
 */

@Getter
public enum ModuleEnum {
    // 脱贫户
    POVERTY_ALLEVIATION(0),
    // 监测户
    POVERTY_MONITORING(1);


    private final int moduleNo;


    ModuleEnum(int moduleNo) {
        this.moduleNo = moduleNo;
    }


    // 根据 moduleNo 获取 ModuleEnum 实例
    public static ModuleEnum fromModuleNo(int moduleNo) {
        for (ModuleEnum module : ModuleEnum.values()) {
            if (module.getModuleNo() == moduleNo) {
                return module;
            }
        }
        throw new IllegalArgumentException("Invalid moduleNo: " + moduleNo);
    }
}
