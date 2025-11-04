### 指标1.2：xxx

### 测试项 1.2.2：通用泛在处理中间件

#### 技术指标

通用泛在数据处理效率提升 50%。

#### 测试方法

TODO

#### 测试用例

- 1.2.2-T1: 泛在数据处理任务构建正确性测试

| 项目 | 内容 |
|------|------|
| **测试目的** | 验证中间件能够智能地将泛在环境中的海量数据变化构建为高效的处理任务批次，测试静态任务构建方法和动态任务构建方法是否能合理组织处理任务，确保任务构建结果符合预期。 |
| **用例初始化** | 准备通用泛在处理中间件开发环境：安装 JDK 17 ，安装 Gradle 8.10 构建工具。|
| **前提和约束** | 已安装 JDK 17；已安装 Gradle 8.10；测试数据和配置文件已位于 `src/test/resources/testcase1/` 目录；oracle 基准文件已准备。 |
| **测试过程终止条件** | 各步骤测试结果和期望结果都一样，则正常终止。若不一致则记录问题，异常终止。 |
| **用例结果评估标准** | 能够成功执行用例且与预期结果一致为"通过"，否则为"不通过" |
| **设计人员** | **To be done** |
| **设计日期** | **To be done** |

**操作过程：**

| 步骤 | 操作 | 预期结果 | 评估准则 |
|------|------|----------|----------|
| 1 | 准备测试环境：<br> `cd /path/to/<TODO>` | 成功进入项目目录 | 与预期结果一致 |
| 2 | 准备测试数据：<br>在 `src/test/resources/testcase1/` 目录下确保以下文件存在：<br>- `Bfunction.java`，`Mfunction.java`，`rules.xml`，`patterns.xml`，`data.txt`（资源文件）<br>- `oracle_taskout_static.txt`（静态任务构建方法基准）<br>- `oracle_taskout_dynamic.txt`（动态任务构建方法基准文件） | 所需文件均存在 | 与预期结果一致 |
| 3 | 运行静态任务构建方法正确性测试：<br> `./gradlew test --tests "TestCase1.T1_STATIC"` | 输出若干执行信息，运行结束后生成任务构建输出文件 `src/test/resources/testcase1/taskout_static.txt`；<br>显示"PASSED"字样（输出文件和基准文件一致） | 与预期结果一致 |
| 4 | 运行动态任务构建方法正确性测试：<br> `./gradlew test --tests "TestCase1.T1_DYNAMIC"` |输出若干执行信息，运行结束后生成任务构建输出文件 `src/test/resources/testcase1/taskout_dynamic.txt`；<br>显示"PASSED"字样（输出文件和基准文件一致） | 与预期结果一致 |


- 1.2.2-T2: 泛在数据处理结果正确性测试

| 项目 | 内容 |
|------|------|
| **测试目的** | 验证中间件可以正确地处理泛在数据。测试增量式泛在数据处理方法和融合式泛在数据处理方法的处理结果是否正确（与基准一致）。 |
| **用例初始化** | 准备通用泛在处理中间件开发环境：安装 JDK 17 ，安装 Gradle 8.10 构建工具。 |
| **前提和约束** | 已安装 JDK 17；已安装 Gradle 8.10；测试数据和配置文件已位于 `src/test/resources/testcase2/` 目录；oracle 基准文件已准备。 |
| **测试过程终止条件** | 各步骤测试结果和期望结果都一样，则正常终止。若不一致则记录问题，异常终止。 |
| **用例结果评估标准** | 能够成功执行用例且与预期结果一致为"通过"，否则为"不通过" |
| **设计人员** | **To be done** |
| **设计日期** | **To be done** |

**操作过程：**

| 步骤 | 操作 | 预期结果 | 评估准则 |
|------|------|----------|----------|
| 1 | 准备测试环境：<br> `cd /path/to/<TODO>` | 成功进入项目目录 | 与预期结果一致 |
| 2 | 准备测试数据：<br>在 `src/test/resources/testcase2/` 目录下确保以下文件存在：<br>- `Bfunction.java`，`Mfunction.java`，`rules.xml`，`patterns.xml`，`data.txt`（资源文件）<br>- `oracle_results.txt`（基准处理结果文件） | 所需文件均存在 | 与预期结果一致 |
| 3 | 运行增量式泛在数据处理方法正确性测试：<br> `./gradlew test --tests "TestCase2.T2_INCREMENTAL"` | 输出若干执行信息，运行结束后生成处理结果文件`src/test/resources/testcase2/results_incremental.txt`.<br>显示"PASSED"字样（结果文件和基准文件一致）| 与预期结果一致 |
| 4 | 运行融合式泛在数据处理方法正确性测试：<br> `./gradlew test --tests "TestCase2.T2_FUSION"` | 输出若干执行信息，运行结束后生成处理结果文件`src/test/resources/testcase2/results_fusion.txt`.<br>显示"PASSED"字样（结果文件和基准文件一致） | 与预期结果一致 |

- 1.2.2-T3: 泛在数据处理效率测试

| 项目 | 内容 |
|------|------|
| **测试目的** | 验证中间件在处理大规模泛在环境数据时的性能表现，确保增量式泛在数据处理方法和融合式泛在数据处理方法的处理效率相比基准方法提升 50% 以上。 |
| **用例初始化** | 准备通用泛在处理中间件开发环境：安装 JDK 17 ，安装 Gradle 8.10 构建工具。 |
| **前提和约束** | 已安装 JDK 17；已安装 Gradle 8.10；测试数据和配置文件已位于 `src/test/resources/testcase3/` 目录。|
| **测试过程终止条件** | 完成所有性能测试场景，收集性能数据。 |
| **用例结果评估标准** | 增量式泛在数据处理方法和融合式泛在数据处理方法相比基准策略性能提升≥50% 为"通过" |
| **设计人员** | **To be done** |
| **设计日期** | **To be done** |

**操作过程：**

| 步骤 | 操作 | 预期结果 | 评估准则 |
|------|------|----------|----------|
| 1 | 准备测试环境：<br> `cd /path/to/<TODO>` | 成功进入项目目录 | 与预期结果一致 |
| 2 | 准备测试数据：<br>在 `src/test/resources/testcase3/` 目录下确保以下文件存在：<br>- `Bfunction.java`，`Mfunction.java`，`rules.xml`，`patterns.xml`，`data.txt`（资源文件） | 所需文件均存在 | 与预期结果一致 |
| 3 | 运行增量式泛在数据处理方法性能测试：<br> `./gradlew test --tests "TestCase3.T3_PERFORMANCE_INCREMENTAL"` | 依次输出基准方法执行时间，增量方法执行时间，输出性能提升百分比，和所减少的时间 | 性能提升百分比超过50% |
| 4 | 运行融合式泛在数据处理方法性能测试：<br> `./gradlew test --tests "TestCase3.T3_PERFORMANCE_FUSION"` | 依次输出基准方法执行时间，增量方法执行时间，输出性能提升百分比，和所减少的时间 | 性能提升百分比超过50% |