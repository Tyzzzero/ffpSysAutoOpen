package org.example.service.impl;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.example.entity.MenuEnum;
import org.example.entity.Model;
import org.example.entity.ModuleEnum;
import org.example.service.BaseActivity;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Tyzzzero
 *
 * 修改联系电话
 */
public class Activity7 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.locator("form").filter(new Locator.FilterOptions().setHasText("家庭成员姓名 成员证件号码 脱贫户状态 查询")).locator("#aab004").click();
        page.locator("form").filter(new Locator.FilterOptions().setHasText("家庭成员姓名 成员证件号码 脱贫户状态 查询")).locator("#aab004").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions()).locator("a").click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtradiobutton span").click();
        page.locator("#aab031").dblclick();
        page.locator("#aab031").fill(model.getCellPhone());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByLabel("户年度基础信息更新").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("")).click();
    }


    public static void main(String[] args) {
        Activity7 activity = new Activity7();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data7.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.BASE_iNFO);
    }
}
