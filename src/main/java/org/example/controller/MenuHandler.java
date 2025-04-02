package org.example.controller;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.entity.MenuEnum;


/**
 * @author Tyzzzero
 */
public class MenuHandler {
    private static final Config config = ConfigLoader.loadConfig(Config.class);


    // 打开菜单
    public void openMenu(Page page, MenuEnum menu) {
        switch (menu) {
            case LABOR_MONITORING -> {
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("务工监测 ")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 信息采集")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 务工监测信息采集")).click();
                page.locator("division-code")
                        .locator("#AAR005 label")
                        // 某些时候打开务工采集页面时间较长，默认配置的超时不满足，未排除原因，针对这步配置超时时间
                        .click(new Locator.ClickOptions().setTimeout(30000));
                page.locator("li").filter(new Locator.FilterOptions().setHasText(config.getTown())).click();
            }
            case BASE_iNFO -> {
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("帮扶对象 ")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 基础信息维护")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 2025年度")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 户年度基础信息更新")).click();
                page.locator("p-panel").locator("#AAR005 label").click();
                page.getByText(config.getTown()).click();
            }
            case YULU_PLAN -> {
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("雨露计划+ ")).click();
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(" 秋季雨露计划+信息采集")).click();
                page.locator("#AAR005 label").click();
                page.getByText(config.getTown()).click();
            }
        }
    }
}
