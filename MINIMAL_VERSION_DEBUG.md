# 最小化版本调试策略

## 概述

根据用户反馈，连续发射火球的问题仍未解决。采用**逐步排查法**，创建最小化功能版本。

---

## 当前版本修改

### ✅ 已禁用的功能

1. **所有附魔系统**
   - 连射 (Multishot)
   - 冰冻 (Freezing)
   - 魅惑 (Charm)
   - 引力奇点 (Gravity Singularity)
   - 滑稽变形 (Polymorph)
   - 穿透追踪 (Piercing Tracker)
   - 持久 (Duration)

2. **效果云系统**
   - 完全注释掉所有效果云检查和生成代码
   - 包括 `checkAndSpawnEffectClouds`
   - 包括 `processCharmClouds`
   - 包括 `processGravityClouds`
   - 包括 `processPolymorphClouds`

### ✅ 保留的核心功能

1. **基础战斗逻辑**
   - `handleCombat()`: 冷却管理 + 目标查找 + 发射控制
   - `findNearestHostile()`: 查找16格内的敌对生物
   - `shootFireballAtTarget()`: **直接创建原版火球，不使用附魔系统**

2. **调试日志**
   ```java
   // 每20 ticks显示一次冷却时间
   System.out.println("[DEBUG] 冷却中: X ticks 剩余");
   
   // 发射前显示详细信息
   System.out.println("[DEBUG] 准备发射火球！");
   System.out.println("[DEBUG] 当前等级: X");
   System.out.println("[DEBUG] 目标: XXX");
   
   // 发射后确认
   System.out.println("[DEBUG] 火球实体创建: 成功/失败");
   System.out.println("[DEBUG] 冷却时间已设置为: X ticks");
   ```

---

## 测试步骤

### 第一阶段：验证最小化版本

1. **启动游戏**，找到/生成一个快乐恶魂
2. **生成怪物**（如僵尸）
3. **观察控制台日志**：
   ```
   [DEBUG] ========================================
   [DEBUG] 准备发射火球！
   [DEBUG] 当前等级: 1
   [DEBUG] 目标: 僵尸
   [DEBUG] 火球实体创建: 成功
   [DEBUG] 冷却时间已设置为: 60 ticks
   [DEBUG] ========================================
   [DEBUG] 冷却中: 60 ticks 剩余
   [DEBUG] 冷却中: 40 ticks 剩余
   [DEBUG] 冷却中: 20 ticks 剩余
   ... (等待冷却完成)
   [DEBUG] 准备发射火球！ (下一次发射)
   ```

4. **检查项目**：
   - ✅ 快乐恶魂是否**只发射一次**，然后等待？
   - ✅ 控制台是否显示冷却时间递减？
   - ✅ 火球是否击中并造成伤害？
   - ✅ 怪物是否正常移动和反击？
   - ✅ 准确度是否正常？

---

## 预期结果

### 如果最小化版本正常 ✅

**说明**：基础战斗系统没有问题，问题出在**附魔系统**或**效果云系统**

**下一步**：逐个启用功能
1. 先启用效果云系统（不启用附魔）
2. 测试效果云是否导致问题
3. 再逐个启用附魔效果：连射 → 冰冻 → 魅惑 → ...

### 如果最小化版本仍有问题 ❌

**说明**：问题在核心战斗逻辑

**可能原因**：
1. `attackCooldown` 字段没有正确保存（每个实例有独立副本）
2. `handleCombat` 被多次调用（服务端+客户端都在调用）
3. `currentTarget` 管理有问题
4. `FireballEntity` 创建本身有问题

**调试方法**：
```java
// 在 handleCombat 开头添加：
System.out.println("[DEBUG] handleCombat 被调用！World类型: " + 
    (ghast.getEntityWorld() instanceof ServerWorld ? "SERVER" : "CLIENT"));
```

---

## 注意事项

1. **查看控制台日志**：游戏窗口下方或 `latest.log` 文件
2. **只测试1级快乐恶魂**：确保冷却时间一致（60 ticks = 3秒）
3. **单独测试一个怪物**：避免多目标干扰
4. **不要使用附魔书**：当前版本已禁用附魔系统

---

## 文件修改位置

`/workspace/src/main/java/me/noramibu/mixin/HappyGhastEntityMixin.java`

1. **Line 216-261**: 添加调试日志到 `handleCombat`
2. **Line 289-320**: `shootFireballAtTarget` 直接创建原版火球
3. **Line 185-211**: 注释所有附魔和效果云处理代码

---

## 编译状态

✅ **BUILD SUCCESSFUL** (2s)

生成文件：`/workspace/build/libs/chest-on-ghast-1.0.0.jar`

---

## 用户测试指南

请按以下步骤测试：

1. **安装新JAR文件**
2. **启动游戏**
3. **生成快乐恶魂和怪物**
4. **观察战斗行为**
5. **查看控制台日志**
6. **报告结果**：
   - 是否还有连续发射问题？
   - 火球是否造成伤害？
   - 怪物是否正常移动？
   - 控制台显示了什么日志？

---

完成测试后，我会根据结果决定下一步行动！🔧
