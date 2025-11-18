# Bug修复报告：ConcurrentModificationException

## 问题描述

在多线程环境下运行INFUSE时，出现`java.util.ConcurrentModificationException`异常。

### 错误堆栈
```
java.util.concurrent.ExecutionException: java.util.ConcurrentModificationException
	at cn.edu.nju.ics.spar.cc.Constraints.Formulas.FForall.removeBranch_INFUSE(FForall.java:1134)
Caused by: java.util.ConcurrentModificationException
	at java.base/java.util.Collection.removeIf(Collection.java:583)
	at cn.edu.nju.ics.spar.cc.Constraints.Formulas.FForall.removeBranch_INFUSE(FForall.java:1134)
```

## 根本原因

### 1. 非线程安全的数据结构
在`RuleHandler.java`的`resolveFormula`方法中，`patToRuntimeNode`使用普通的`HashSet`初始化：
```java
patToRunTimeNode.put(eFormula.attributeValue("in"), new HashSet<>());  // 第61行和71行
```

### 2. 并发访问冲突
- `INFUSE_C`使用线程池(13个线程)并发执行`ModifyBranchTask_INFUSE`
- 多个线程同时访问和修改`rule.getPatToRuntimeNode()`中的同一个`HashSet`
- 以下位置存在并发访问：
  - **FForall.java:1134** - `removeIf`操作
  - **FForall.java:1150** - `add`操作  
  - **FExists.java:1123** - `removeIf`操作
  - **FExists.java:1136** - `add`操作

### 3. 调用链
```
OfflineStarter.run()
└─> INFUSE_S.checkEnds()
    └─> INFUSE_S.CleanUp()
        └─> INFUSE_C.ctxChangeCheckBatch()
            └─> Rule.modifyCCT_INFUSE()
                └─> FForall/FExists.modifyBranch_INFUSE() [多线程并发]
                    └─> removeBranch_INFUSE()
                        └─> Set.removeIf() ❌ ConcurrentModificationException
```

## 修复方案

### 最终方案：使用 ConcurrentHashMap.newKeySet()（已实现）✅

使用线程安全的 Set 实现替换普通的 HashSet，从根源上解决并发问题。

#### 修改1：RuleHandler.java - 导入依赖（第15行）
```java
import java.util.concurrent.ConcurrentHashMap;
```

#### 修改2：RuleHandler.java - forall 初始化（第62行）
```java
case "forall":{
    // ...
    // 使用线程安全的 Set 实现，支持并发访问
    patToRunTimeNode.put(eFormula.attributeValue("in"), ConcurrentHashMap.newKeySet());
    // ...
}
```

#### 修改3：RuleHandler.java - exists 初始化（第73行）
```java
case "exists":{
    // ...
    // 使用线程安全的 Set 实现，支持并发访问
    patToRunTimeNode.put(eFormula.attributeValue("in"), ConcurrentHashMap.newKeySet());
    // ...
}
```

## 相关文件

- ✏️ `src/main/java/cn/edu/nju/ics/spar/cc/Constraints/Rules/RuleHandler.java` (修复位置)
- 📖 `src/main/java/cn/edu/nju/ics/spar/cc/Constraints/Formulas/FForall.java` (调用位置)
- 📖 `src/main/java/cn/edu/nju/ics/spar/cc/Constraints/Formulas/FExists.java` (调用位置)
- 📖 `src/main/java/cn/edu/nju/ics/spar/cc/Constraints/Rules/Rule.java` (数据结构定义)
- 📖 `src/main/java/cn/edu/nju/ics/spar/cc/Middleware/Checkers/INFUSE_C.java` (并发执行)
- 📖 `src/main/java/cn/edu/nju/ics/spar/cc/Middleware/Schedulers/INFUSE_S.java` (调度逻辑)

## 修复日期
2025年11月18日

