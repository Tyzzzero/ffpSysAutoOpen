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

/**
 * @author Tyzzzero
 *
 * 根据务工开始时间定位务工，修改务工终止时间
 */
public class Activity11 extends BaseActivity {
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
        // 在务工记录面板内查找datatable元素，表格是在组件内呈现，同样按实际HTML结构对应的选择器来定位
        Locator dataTable = workRecordPanel.locator("p-datatable");
        // 获取表格内所有行元素，行元素tr标签
        // 务工记录开始年月具有唯一性，根据"开始年月"匹配记录所在行，点击所在行前按钮
        Locator row = dataTable.locator("tr").filter(new Locator.FilterOptions().setHasText(model.getStartDate().trim()));
        Locator radioButtonIcon = row.locator(".ui-radiobutton-icon");
        radioButtonIcon.click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("修改务工记录")).click();
        page.locator("#aab086 label").click();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy年M月");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy年MM月");
        YearMonth ymEnd = YearMonth.parse(model.getEndDate().trim(), inputFormatter);
        page.locator("li").filter(new Locator.FilterOptions().setHasText(ymEnd.format(outputFormatter))).click();
        page.locator("#on_save2").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByLabel("务工监测信息采集").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("")).click();
    }


    public static void main(String[] args) {
        Activity11 activity = new Activity11();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data1.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.LABOR_MONITORING);
    }
}
