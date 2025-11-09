# 战斗系统Bug修复报告

## 发现的Bug

测试时发现以下严重问题：

### Bug 1: 快乐恶魂疯狂射击，没有冷却时间 🔥
**现象**：
- 快乐恶魂持续不停地发射火球
- 冷却时间似乎不生效
- 大量火球同时存在

**原因分析**：
1. **客户端/服务端同步问题**：火球在客户端和服务端都被创建了
2. AI Goal的`tick()`方法在客户端和服务端都会执行
3. 导致每帧都创建两个火球（客户端1个+服务端1个）

**修复方法**：
添加服务端检查，只在服务端创建火球：

```java
// AttackHostilesGoal.java - tick()方法
if (this.ghast.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld) {
    shootFireball();
    this.fireballCooldown = LevelConfig.getAttackCooldown(currentLevel);
}
```

```java
// EnchantmentHelper.java - shootSingleFireball()和shootMultipleFireballs()
if (!(ghast.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld)) {
    return;  // 只在服务端创建火球
}
```

### Bug 2: 火球没有伤到任何怪物 💔
**现象**：
- 火球能正常发射和爆炸
- 但不会对怪物造成任何伤害
- 地形可能会被破坏，但怪物不受影响

**可能原因分析**：
1. **火球威力(explosionPower)设置问题**：
   - 配置文件中的fireballPower值为1-6
   - Minecraft原版恶魂火球的explosionPower=1
   - 这个值应该是合理的

2. **实体所有者(owner)问题**：
   - 火球的owner是快乐恶魂
   - 但快乐恶魂可能和怪物在同一个队伍
   - 导致"友军伤害"被禁用

3. **快乐恶魂实体类型问题**：
   - HappyGhastEntity可能被识别为"友好生物"
   - Minecraft可能禁止友好生物的火球伤害敌对生物

**建议排查步骤**：
1. 检查快乐恶魂的实体类型定义
2. 检查快乐恶魂是否有队伍(Team)设置
3. 测试原版恶魂的火球是否能伤害怪物
4. 检查fire球实体的owner设置是否正确

**可能的解决方案**：
需要检查`HappyGhastEntity`的定义，确保：
- 它继承自正确的基类
- 没有被错误地设置为"友好"类型
- 火球的owner正确设置

### Bug 3: 有些怪物静止不动 🧊
**现象**：
- 部分怪物在生成后不移动
- 不是所有怪物都这样
- 看起来像被冻结了

**可能原因分析**：

#### 可能性1：冰冻附魔Bug
如果装备了"冰冻"附魔：
- 冰冻效果可能过度应用
- slowness和mining_fatigue效果可能过强
- 检查代码：`HappyGhastEntityMixin.addFreezingEffect()`

#### 可能性2：魅惑附魔Bug
如果装备了"魅惑"附魔：
- 怪物可能在等待攻击目标
- AI可能被破坏
- 检查代码：`HappyGhastEntityMixin.applyCharmEffect()`

#### 可能性3：变形附魔Bug
如果装备了"滑稽变形"附魔：
- 怪物可能已经被变形但实体未正确删除
- 检查代码：`HappyGhastEntityMixin.polymorphHostileToPassive()`

**诊断方法**：
1. **移除所有附魔书** - 测试是否还有怪物静止
2. **逐个测试附魔** - 确定是哪个附魔导致的
3. **检查怪物AI** - 使用F3+B查看怪物的碰撞箱

## 已修复的问题

### ✅ Bug 1: 客户端/服务端重复发射
**状态**：已修复

**修改文件**：
1. `/workspace/src/main/java/me/noramibu/ai/AttackHostilesGoal.java`
   - 第88行添加服务端检查

2. `/workspace/src/main/java/me/noramibu/enchantment/EnchantmentHelper.java`
   - 第67行：`shootSingleFireball()`添加服务端检查
   - 第133行：`shootMultipleFireballs()`添加服务端检查

**预期效果**：
- 火球只在服务端创建一次
- 冷却时间正常生效
- 不再疯狂射击

## 待修复的问题

### ⚠️ Bug 2: 火球无伤害
**状态**：需要进一步排查

**建议**：
1. 检查`HappyGhastEntity`的实体定义
2. 可能需要查看原版代码的恶魂实现
3. 可能需要Mixin到FireballEntity的伤害逻辑

### ⚠️ Bug 3: 怪物静止
**状态**：需要玩家测试

**测试步骤**：
1. 移除所有附魔书
2. 生成怪物，观察是否还会静止
3. 如果不静止，逐个测试附魔效果
4. 确定具体是哪个附魔导致的

## 测试建议

### 测试1：基础战斗系统
```
1. 生成一个1级快乐恶魂（无附魔）
2. 生成几只僵尸
3. 观察：
   - 火球发射频率（应该是3秒一次）
   - 火球是否能击中僵尸
   - 火球是否造成伤害
   - 僵尸是否正常移动
```

### 测试2：冷却时间
```
1. 生成不同等级的快乐恶魂
2. 计时每次发射间隔
3. 对比配置文件：
   - 1级：60 ticks (3秒)
   - 2级：50 ticks (2.5秒)
   - 3级：40 ticks (2秒)
   - 4级：30 ticks (1.5秒)
   - 5级：20 ticks (1秒)
   - 6级：15 ticks (0.75秒)
```

### 测试3：附魔效果
```
逐个测试每种附魔：
1. ✅ 连射：应该发射多个火球
2. ✅ 持久：效果云持续时间更长
3. ⚠️ 冰冻：怪物是否被冻住过度？
4. ⚠️ 魅惑：怪物是否正常互相攻击？
5. ⚠️ 变形：怪物是否正确变成动物？
6. ✅ 引力奇点：怪物是否被吸引？
```

## 配置文件建议

如果火球伤害太低，可以调整配置：

```json
{
  "levels": {
    "1": {
      "fireballPower": 2,  // 增加威力
      "attackCooldown": 60
    },
    "6": {
      "fireballPower": 8,  // 最高级大幅增加
      "attackCooldown": 15
    }
  }
}
```

## 调试日志

代码中已添加调试日志（目前注释掉）：

```java
// AttackHostilesGoal.java:96
// System.out.println("[AttackHostilesGoal] 发射火球！冷却时间：" + this.fireballCooldown + " ticks");

// EnchantmentHelper.java:93
// System.out.println("[EnchantmentHelper] 火球生成: " + spawned + ", Power: " + fireballPower);
```

如果需要调试，可以取消注释这些行。

## 结论

已成功修复"疯狂射击"问题，但"火球无伤害"和"怪物静止"问题需要进一步测试和排查。

**建议玩家**：
1. 先测试基础战斗系统（无附魔）
2. 观察火球是否能正常造成伤害
3. 如果仍无伤害，需要深入检查实体定义
4. 测试附魔效果，确定哪个导致怪物静止

---
**Minecraft 快乐恶魂 Mod v1.0.3**
**Bug修复日期**: 2025-11-09
