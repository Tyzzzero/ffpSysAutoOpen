## ffpSysAutoOpen是什么?

一个用于`全国防止返贫监测和衔接推进乡村振兴信息系统`批量数据自动化工具。它可以帮助用户自动完成系统中的批量数据操作，如务工信息修改、基础信息更新等。

## ffpSysAutoOpen有哪些功能？

* 🎯 支持登录验证码自动识别
* 📝 支持务工模块，基础信息模块，雨露计划模块常见功能批量修改
* 🔧 轻松实现自定义扩展
* 🔄 支持失败重试，可无人值守
* 📊 支持处理结果名单输出
* ⚡ 支持多线程处理，提高效率
* 📋 自动记录执行日志，方便追踪问题

## 项目结构说明

```
ffpSysAutoOpen/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── example/
│   │   │           ├── config/          # 配置相关
│   │   │           │   ├── Config.java  # 系统配置类
│   │   │           │   └── ConfigLoader.java  # 配置加载器
│   │   │           ├── entity/          # 实体类
│   │   │           │   ├── Model.java   # 数据模型
│   │   │           │   ├── MenuEnum.java  # 菜单枚举
│   │   │           │   └── ModuleEnum.java  # 模块枚举
│   │   │           ├── service/         # 服务类
│   │   │           │   ├── BaseActivity.java  # 基础活动类
│   │   │           │   └── impl/        # 具体实现
│   │   │           └── utils/           # 工具类
│   │   └── resources/
│   │       ├── config.properties        # 配置文件
│   │       └── logback.xml              # 日志配置
├── target/
│   └── output/                          # 输出目录
└── pom.xml                              # Maven项目配置
```

### 主要文件说明

1. **`Config.java`**
   - 系统配置类，包含所有可配置项
   - 使用 `@Required` 注解标记必填项
   - 提供配置项验证功能

2. **`Model.java`**
   - 数据模型类，对应Excel表格结构
   - 使用 `@ExcelProperty` 注解映射Excel列
   - 包含数据验证和格式化功能

3. **`BaseActivity.java`**
   - 基础活动类，提供核心功能
   - 处理多线程、重试、日志等
   - 需要继承并实现 `onStart` 方法

4. **`config.properties`**
   - 系统配置文件
   - 包含系统URL、账号密码等配置
   - 支持线程数、超时时间等参数调整

5. **`ModuleEnum.java`**
   - 系统模块枚举类，定义可访问的子系统
   - 当前支持的模块：
     - `POVERTY_ALLEVIATION`: 巩固脱贫攻坚成果子系统
     - `MONITORING`: 防返贫监测子系统
   - 可以根据需要扩展新的模块
   - 使用示例：
     ```java
     activity.execute("data1.xlsx", ModuleEnum.POVERTY_ALLEVIATION, MenuEnum.YOUR_MENU);
     ```

6. **`MenuEnum.java`**
   - 菜单枚举类，定义各子系统下的功能菜单
   - 当前支持的菜单：
     - `LABOR_MONITORING`: 务工监测
     - `BASIC_INFO`: 基础信息
     - `RAIN_PLAN`: 雨露计划
   - 可以根据需要扩展新的菜单
   - 使用示例：
     ```java
     activity.execute("data1.xlsx", ModuleEnum.YOUR_MODULE, MenuEnum.LABOR_MONITORING);
     ```

## 使用步骤

### 1. 环境准备

#### 1.1 安装Node.js
1. 访问 [Node.js官网](https://nodejs.org/zh-cn)
2. 下载并安装LTS版本
3. 验证安装：打开命令行，输入 `node -v` 和 `npm -v`

#### 1.2 安装Playwright
1. 打开命令行终端
2. 执行命令：
   ```bash
   npm i playwright
   npx playwright install
   ```

#### 1.3 安装IDEA
1. 下载 [IDEA Community Edition](https://www.jetbrains.com/zh-cn/idea/download/download-thanks.html?platform=windows&code=IIC)
2. 安装并启动IDEA

### 2. 项目配置

#### 2.1 导入项目
1. 下载本项目并解压
2. 打开IDEA，选择"Open"
3. 选择解压后的项目文件夹

#### 2.2 配置系统参数
1. 打开 `src/main/resources/config.properties`
2. 填写必填项：
   ```
   # 系统URL（必填）
   url=http://your-system-url
   
   # 乡镇级账号登录用户名（必填）
   userName=your-username
   
   # 乡镇级账号登录密码（必填）
   password=your-password
   ```
3. 根据需要调整其他参数：
   ```
   # 执行失败重试次数（默认3）
   retryCount=3
   
   # 全局超时时间（毫秒，默认8000）
   globalTimeout=8000.0
   
   # 线程数（默认1，范围1-10）
   threadCount=1
   
   # 验证码最大尝试次数（默认5，范围1-10）
   maxCaptchaAttempts=5
   ```

#### 2.3 准备数据文件
1. 在 `src/main/resources` 下创建Excel文件（如 `data1.xlsx`）
2. Excel文件的列名需要与 `Model.java` 中的字段名保持一致，可以根据实际需求增减字段
3. 示例数据格式（根据实际需求调整字段）：
   | id  | name | idCard             | amount | startDate | endDate    | cellPhone   | province | city   | county |
   | --- | ---- | ------------------ | ------- | --------- | ---------- | ----------- | -------- | ------ | ------ |
   | 1   | 张三 | 510104***1499 | 1500    | 2025年1月 | 2024年12月 | 133***5678 | 四川省   | 成都市 | 锦江区 |

### 3. 开发自定义功能

> **<span style="color: #0000FF">📌 快速开始</span>**
> 
> 在 `src/main/java/org/example/service/impl` 目录下有简单的实现类，可以直接参考使用。这些实现类已经包含了常见功能的处理逻辑，可以根据需要进行修改或扩展。
> 
> **<span style="color: #FF0000">⚠️ 注意</span>**
> 
> 如现有实现类不符合需求，可参考以下方法，开发自定义实现类：

#### 3.1 录制操作脚本
1. 打开命令行，执行：
   ```bash
   npx playwright codegen http://your-system-url
   ```
2. 在浏览器中完成需要自动化的操作
3. 在`Playwright Inspector`中选择`Target`->`Java`->`Library`
4. 复制生成的代码

#### 3.2 代码组织原则
在创建`BaseActivity`的继承类时，需要特别注意代码的组织方式。录制的脚本通常包含以下三个部分：

1. **初始化部分**（在 `BaseActivity` 中）
   - 登录系统
   - 进入指定子系统
   - 打开指定菜单
   - 这些操作只需要执行一次，由 `BaseActivity` 统一处理
   - **<span style="color: #FF0000">除非有特殊需求，否则不需要修改这部分代码</span>**

2. **循环部分**（放在`BaseActivity` 实现类的`onStart` 方法中）
   - 使用 `model` 中的数据填充表单
   - 提交表单
   - 处理结果
   - 这些操作会针对每条数据重复执行
   - **<span style="color: #008000">这是开发者需要重点关注和实现的部分</span>**

3. **清理部分**（在 `BaseActivity` 中）
   - 关闭浏览器
   - 保存结果
   - 这些操作在程序结束时执行
   - **<span style="color: #FF0000">除非有特殊需求，否则不需要修改这部分代码</span>**

> **<span style="color: #0000FF">重要说明</span>**：在大多数情况下，开发者只需要：
> 1. 创建继承自 `BaseActivity` 的类
> 2. 实现 `onStart` 方法，处理具体的数据操作
> 3. **<span style="color: #FF0000">不需要关心初始化和清理部分的实现</span>**
> 4. **<span style="color: #FF0000">在循环体（onStart方法）中不需要添加登录和进入菜单的逻辑，这些操作由BaseActivity统一处理</span>**

示例说明：

**<span style="color: #FF0000">❌ 错误示例</span>**：在onStart中包含所有操作
```java
public class YourActivity extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        // 登录系统（错误：这部分应该放在BaseActivity中）
        page.goto(config.getUrl());
        page.fill("#username", config.getUserName());
        // ...
        
        // 进入菜单（错误：这部分应该放在BaseActivity中）
        page.click("#menu1");
        page.click("#submenu1");
        
        // 处理数据（正确：这部分应该放在`BaseActivity`继承类的onStart中）
        page.locator("#aab004").fill(model.getIdCard());
        // ...
    }
}
```

**<span style="color: #008000">✅ 正确示例</span>**：onStart只包含数据处理部分
```java
public class YourActivity extends BaseActivity {
    @Override
    protected void onStart(Page page, Model model) {
        // 使用model中的数据填充表单，如在搜索框输入证件号码和姓名
        page.locator("#aab004").fill(model.getIdCard());
        page.locator("#aab005").fill(model.getName());
        // ... 其他数据处理操作
    }
}
```

#### 3.3 创建实现类
1. 在 `src/main/java/org/example/service/impl` 下创建新类
2. 继承 `BaseActivity` 类
3. 实现 `onStart` 方法，只包含数据处理部分的代码
4. 示例：
   ```java
   @Override
   protected void onStart(Page page, Model model) {
       // 使用model中的数据填充表单，如在搜索框输入证件号码和姓名
       page.locator("#aab004").fill(model.getIdCard());
       // ... 其他数据处理操作
   }
   ```

#### 3.4 执行程序
1. 在实现类中添加main方法：
   ```java
   public static void main(String[] args) {
       YourActivity activity = new YourActivity();
       activity.execute("data1.xlsx", ModuleEnum.YOUR_MODULE, MenuEnum.YOUR_MENU);
   }
   ```
2. 运行main方法

### 4. 查看结果

1. 程序执行完成后，在 `target/output` 目录下查看结果文件：
   - `yyyyMMddHHmmss_result.xlsx`: 包含三个sheet页
     - "失败数据": 处理失败的数据
     - "成功数据": 处理成功的数据
     - "全部数据": 所有处理的数据
   - 文件名中的时间戳格式为：年月日时分秒

2. 查看日志输出，了解执行情况：
   - 总处理时间（格式：xx小时xx分钟xx秒xx毫秒）
   - 成功/失败数量
   - 具体错误信息

## 常见问题

1. **配置问题**
   - 确保 `url`、`userName` 和 `password` 已正确配置
   - 检查网络连接是否正常
   - 验证账号是否有足够权限

2. **数据问题**
   - 确保Excel文件格式正确
   - 检查数据是否完整
   - 验证数据格式是否符合要求

3. **执行问题**
   - 检查系统响应时间，适当调整 `globalTimeout`
   - 根据系统性能调整 `threadCount`
   - 查看日志定位具体错误

## 注意事项

1. 配置文件中的 `url`、`userName` 和 `password` 为必填项，未配置将导致程序无法运行
2. 数值类型的配置项（如 `retryCount`、`globalTimeout`、`threadCount`、`maxCaptchaAttempts`）有默认值和范围限制，超出范围将自动调整为默认值
3. 程序会自动记录执行日志，包括处理时间、成功/失败数量等信息
4. 支持多线程处理，但建议根据系统性能合理设置线程数
5. 建议在非高峰期执行批量操作
6. 定期检查日志，及时发现和处理问题