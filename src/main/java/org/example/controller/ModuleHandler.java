package org.example.controller;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.example.entity.ModuleEnum;


/**
 * @author Tyzzzero
 */
public class ModuleHandler {


    // 打开模块
    public void openModule(Page page, ModuleEnum module) {
        switch (module) {
            case POVERTY_ALLEVIATION -> {
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("巩固脱贫攻坚成果子系统")).click();
                page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("电信地址")).click();
            }
            case POVERTY_MONITORING -> {
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("防返贫监测子系统")).click();
                page.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText("电信地址")).click();
            }
        }
    }
}