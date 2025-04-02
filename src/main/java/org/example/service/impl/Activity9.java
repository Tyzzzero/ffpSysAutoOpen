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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Tyzzzero
 *
 * 新增务工
 */
public class Activity9 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.locator("#aab004").click();
        page.locator("#aab004").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.waitForTimeout(800);
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions()).locator("a").click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtradiobutton span").click();
        page.waitForTimeout(800);

        // 新增务工
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新增务工记录")).click();
        page.waitForTimeout(500);
        page.locator("#aab211 span").click();
        page.locator("li").filter(new Locator.FilterOptions().setHasText("其他形式务工就业人员")).nth(1).click();
        page.locator("#aab082 label").click();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy年M月");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        YearMonth ymStart = YearMonth.parse(model.getStartDate().trim(), inputFormatter);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(ymStart.format(outputFormatter))).click();
        page.locator("#aab025 label").click();
        page.waitForTimeout(500);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(model.getProvince().trim())).locator("span").click();
        page.locator("#aab026 label").click();
        page.waitForTimeout(500);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(model.getCity().trim())).click();
        page.locator("#aab027 label").click();
        page.waitForTimeout(500);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(model.getCounty().trim())).click();
        page.locator("#aab087").click();
        page.locator("#aab087").fill("2500");
        page.locator("#aab089 label").click();
        page.waitForTimeout(500);
        page.locator("li").filter(new Locator.FilterOptions().setHasText("第三产业")).locator("span").click();
        page.locator("#aab215 label").click();
        page.waitForTimeout(500);
        page.locator(".ui-dropdown-items > li:nth-child(7)").click();
        page.locator("#aab093").getByTitle("　").locator("label").click();
        page.waitForTimeout(500);
        page.locator("#aab093 li").filter(new Locator.FilterOptions().setHasText("其他形式就业")).locator("span").click();
        page.locator("#on_save2").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByLabel("务工监测信息采集").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("")).click();
    }


    public static void main(String[] args) {
        Activity9 activity = new Activity9();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data9.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.LABOR_MONITORING);
    }
}
