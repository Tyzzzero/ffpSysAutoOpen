
## ffpSysAutoOpen是什么?

一个用于`全国防止返贫监测和衔接推进乡村振兴信息系统`批量数据自动化工具

## ffpSysAutoOpen有哪些功能？

* 支持登录验证码自动识别
* 支持务工模块，基础信息模块，雨露计划模块常见功能批量修改
* 轻松实现自定义扩展
* 支持失败重试，可无人值守
* 支持处理结果名单输出

## 使用步骤

### 本地环境配置

#### 安装[node.js](https://nodejs.org/zh-cn)

官网下载安装包，默认配置安装


#### 安装[playwright](https://github.com/microsoft/playwright)

打开命令行终端，执行命令


```
npm i playwright
```

#### 安装浏览器依赖

打开命令行终端，执行命令

```
npx playwright install
```

### 脚本录制

打开命令行终端, 输入命令，`<url>`替换为`系统网址`（各地区可能不同），执行命令

```
npx playwright codegen <url>
```
录制完成后，在`Playwright Inspector`窗口中，选择`Target`->`Java`->`Library`，获得Java代码


### 安装[IDEA Community Edition](https://www.jetbrains.com/zh-cn/idea/download/download-thanks.html?platform=windows&code=IIC)

官网下载安装包，默认配置安装

### 下载导入项目

下载本项目，解压缩，导入IDEA


### 项目文件配置
#### 修改config.properties

修改`src/main/resources`路径下`config.properties`

```
# 网站链接
url=
    
# 县级账号登录用户名
userName=
    
# 县级账号登录密码
password=
    
# 默认使用县级账号登录，查询时选择所在乡镇，如"**镇"、"**乡"
town=
    
# 执行失败重试次数
retryCount=3

# 全局超时时间
globalTimeout=5000.0
```

#### 新建基础数据文件
* 在`src/main/resources`路径下，新建`.xlsx`文件，所需字段根据功能增加或删减，比如新建`data1.xlsx`，内容如下：

| id  | name | idCard             | amount | startDate | endDate    | cellPhone   | province | city   | county |
| --- | ---- | ------------------ | ------- | --------- | ---------- | ----------- | -------- | ------ | ------ |
| 1   | 张三 | 510104***1499 | 1500    | 2025年1月 | 2024年12月 | 133***5678 | 四川省   | 成都市 | 锦江区 |
| 2   | 李四 | 510108***2712 | 1800    | 2025年1月 | 2024年12月 | 134***5678 | 四川省   | 成都市 | 成华区 |


* 修改`src/main/java/org/example/entity`路径下`Model.java`，属性与`.xlsx`文件表头对应，如下

```Java
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class Model {
    /**
     * 行编号
     */
    @ExcelProperty
    private long id;
    /**
     * 姓名
     */
    @ExcelProperty
    private String name;
    /**
     * 证件号码
     */
    @ExcelProperty
    private String idCard;
    /**
     * 补贴金额/收入金额
     */
    @ExcelProperty
    private int amount;
    /**
     * 务工开始年月
     */
    @ExcelProperty
    private String startDate;
    /**
     * 务工结束年月
     */
    @ExcelProperty
    private String endDate;
    /**
     * 联系方式
     */
    @ExcelProperty
    private String cellPhone;
    /**
     * 省份
     */
    @ExcelProperty
    private String province;
    /**
     * 城市
     */
    @ExcelProperty
    private String city;
    /**
     * 区县
     */
    @ExcelProperty
    private String county;
}
```

#### 继承BaseActivity抽象类
* 继承`org/example/service`路径下`BaseActivity.java`，实现`onStart`方法，将Playwright录制代码放入修改（多数代码来自于Playwright录制，只需完善定位代码以及填充逻辑代码），可参考`src/main/java/org/example/service/impl`路径下实现类，，如下：

#### 创建实现类对象并执行
* 创建实现类对象, 执行实现类方法`execute()`，可参考`src/main/java/org/example/service/impl`路径下实现类，如下：

```Java
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
 * 终止务工，并新增务工
 */
public class Activity8 extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        // 此处只填充循环部分逻辑，比如进入务工菜单只在首次进入，不需要每个人查询都重复打开菜单，录制的登录以及打开菜单部分代码不填充在这里
        // 下面开始根据不同的身份证件号信息开始查询
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
        page.locator("#aab087").fill(String.valueOf(model.getAmount()));
        page.locator("#aab089 label").click();
        page.locator("li").filter(new Locator.FilterOptions().setHasText("第三产业")).locator("span").click();
        page.locator("#aab215 label").click();
        page.locator(".ui-dropdown-items > li:nth-child(7)").click();
        page.locator("#aab093").getByTitle("　").locator("label").click();
        page.locator("#aab093 li").filter(new Locator.FilterOptions().setHasText("其他就业形式")).locator("span").clic();
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
```

### 获取执行结果名单
* 在`target/output`路径下，带`_all`后缀为全量名单，带`_failure`后缀为执行失败名单，带`_success`后缀为执行成功名单