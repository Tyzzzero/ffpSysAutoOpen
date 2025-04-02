
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


#### 安装[playwright](https://github.com/microsoft/playwright)
```javascript
npm i playwright
```

#### 安装浏览器依赖
```javascript
npx playwright install
```

### 脚本录制
```javascript
npx playwright codegen <url>
```
录制完成后，在`Playwright Inspector`窗口中，选择`Target`->`Java`->`Library`，获得通用Java代码

### 项目文件配置
#### 修改config.properties
```javascript
# 链接
url=
    
# 登录用户名
userName=
    
# 登录密码
password=
    
# 默认使用县级账号登录，查询时选择所在乡镇，如"**镇"、"**乡"
town=
    
# 执行失败重试次数
retryCount=3

# 全局超时时间
globalTimeout=5000.0
```
#### 继承BaseActivity抽象类

* 继承`org/example/service`路径下`BaseActivity.java`，实现`onStart`方法，将playwright录制代码放入修改

#### 新建基础数据文件

* 在`src/main/resources`路径下，新建`.xlsx`文件
* 修改`src/main/java/org/example/entity`路径下`Model.java`，属性与`.xlsx`文件表头对应

#### 创建实现类对象
* 创建实现类对象, 执行实现类方法`execute()`

#### 执行结果名单输出
* 在`target/output`路径下，带`_all`后缀为全量名单，带`_failure`后缀为执行失败名单，带`_success`后缀为执行成功名单