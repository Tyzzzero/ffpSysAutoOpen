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
 * 终止务工，并新增务工
 */
public class Activity8 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.locator("#aab004").click();
        page.locator("#aab004").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.waitForTimeout(500);
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions()).locator("a").click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtradiobutton span").click();
        page.waitForTimeout(500);

        // 终止务工
        Locator workRecordPanel = page.locator("p-panel[header='务工记录']");
        // 在务工记录面板内查找datatable元素，表格在这个组件内呈现，同样按实际HTML结构对应的选择器来定位
        Locator dataTable = workRecordPanel.locator("p-datatable");
        // 获取表格内所有行元素，行元素是tr标签
        List<Locator> rows = dataTable.locator("tr").all();
        if (rows.size() > 0) {
            // 获取最后一行元素
            Locator lastRow = rows.get(rows.size() - 1);
            // 在最后一行里查找ui-radiobutton-icon元素，根据其所在的HTML结构层级，从最后一行元素基础上继续定位
            Locator radioButtonIcon = lastRow.locator(".ui-radiobutton-icon");
            if (radioButtonIcon.isVisible()) {
                // 点击该图标元素
                radioButtonIcon.click();
            }
        }
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("修改务工记录")).click();
        page.locator("#aab086 label").click();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy年M月");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        YearMonth ymEnd = YearMonth.parse(model.getEndDate().trim(), inputFormatter);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(ymEnd.format(outputFormatter))).click();
        page.locator("#on_save2").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();

        // 新增务工
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新增务工记录")).click();
        page.waitForTimeout(500);
        page.locator("#aab211 span").click();
        page.locator("li").filter(new Locator.FilterOptions().setHasText("单位录用聘用人员")).nth(1).click();
        page.locator("#aab082 label").click();
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
        page.locator("#aab087").fill(String.valueOf(model.getSubsidy()));
        page.locator("#aab089 label").click();
        page.waitForTimeout(500);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(model.getChannel1().trim())).locator("span").click();
        page.locator("#aab215 label").click();
        page.waitForTimeout(500);
        page.locator(".ui-dropdown-items > li:nth-child(7)").click();
        page.locator("#aab093").getByTitle("　").locator("label").click();
        page.locator("#aab093 li").filter(new Locator.FilterOptions().setHasText(model.getChannel3().trim())).locator("span").click();
        page.locator("#on_save2").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByLabel("务工监测信息采集").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("")).click();
    }


    public static void main(String[] args) {
        Activity8 activity = new Activity8();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data8.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.LABOR_MONITORING);
    }
}
