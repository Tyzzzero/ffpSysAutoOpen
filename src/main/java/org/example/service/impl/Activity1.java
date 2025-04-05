package org.example.service.impl;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.example.entity.MenuEnum;
import org.example.entity.Model;
import org.example.entity.ModuleEnum;
import org.example.service.BaseActivity;

/**
 * @author Tyzzzero
 *
 * 修改务工补贴
 */
public class Activity1 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.locator("#aab004").click();
        page.locator("#aab004").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions()).locator("a").click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtradiobutton span").click();
        page.locator("#aab094").click();
        page.locator("#aab094").fill(String.valueOf(model.getAmount()));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByLabel("务工监测信息采集").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("")).click();
    }


    public static void main(String[] args) {
        Activity1 activity = new Activity1();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data1.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.LABOR_MONITORING);
    }
}
