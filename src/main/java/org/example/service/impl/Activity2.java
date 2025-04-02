package org.example.service.impl;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.example.entity.MenuEnum;
import org.example.entity.Model;
import org.example.entity.ModuleEnum;
import org.example.service.BaseActivity;

import java.util.List;

/**
 * @author Tyzzzero
 *
 * 修改务工记录月收入
 */
public class Activity2 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.locator("#aab004").click();
        page.locator("#aab004").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.waitForTimeout(1500);
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions()).locator("a").click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtradiobutton span").click();
        page.waitForTimeout(1500);
        Locator workRecordPanel = page.locator("p-panel[header='务工记录']");
        // 在务工记录面板内查找datatable元素，因为表格是在这个组件内呈现的，同样按实际HTML结构对应的选择器来定位
        Locator dataTable = workRecordPanel.locator("p-datatable");
        // 获取表格内所有行元素，这里假设行元素是tr标签（根据实际HTML结构看确实如此）
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
        page.locator("#aab087").dblclick();
        page.locator("#aab087").fill("1500");
        page.locator("#on_save2").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
    }


    public static void main(String[] args) {
        Activity2 activity = new Activity2();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data2.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.LABOR_MONITORING);
    }
}
