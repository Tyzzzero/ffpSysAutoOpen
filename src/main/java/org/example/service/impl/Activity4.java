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
 * 修改秋季雨露计划学校性质与补贴金额
 */
public class Activity4 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        page.getByPlaceholder("请输入证件号码").dblclick();
        page.getByPlaceholder("请输入证件号码").fill(model.getIdCard().trim());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("查询")).click();
        page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(model.getName().trim())).locator("p-dtcheckbox span").click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("修改")).click();
        page.getByPlaceholder("保留2位小数").click();
        page.getByPlaceholder("保留2位小数").fill("1500");
        page.locator("#aab232 label").click();
        page.locator("li").filter(new Locator.FilterOptions().setHasText("公办")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("保存")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
    }


    public static void main(String[] args) {
        Activity4 activity = new Activity4();
        // 执行时传入输入文件名、模块编号和菜单编号
        activity.execute("data4.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.YULU_PLAN);
    }
}
