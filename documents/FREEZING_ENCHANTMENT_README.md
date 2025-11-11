# 冰冻附魔 (Freezing Enchantment)

## 概述

冰冻附魔是快乐恶魂火球附魔系统的第二个实现的附魔效果。当快乐恶魂的火球生成效果云时，冰冻附魔会让效果云冻住怪物，使其无法移动和攻击。

## 附魔效果

冰冻附魔会在效果云中添加强力的减速和挖掘疲劳效果，让怪物几乎完全无法行动：

### 等级效果

| 等级 | 冻结时长 | 缓慢等级 | 效果描述 |
|------|---------|---------|---------|
| I    | 3秒     | 缓慢V    | 基本无法移动 |
| II   | 5秒     | 缓慢VII  | 完全冻结 |
| III  | 8秒     | 缓慢X    | 超级冻结 |

### 效果组合

冰冻效果云会同时施加以下状态效果：

1. **缓慢（Slowness）**
   - 大幅降低怪物移动速度
   - 等级越高，移动速度越慢

2. **挖掘疲劳（Mining Fatigue）**
   - 防止怪物快速攻击
   - 配合缓慢效果，让怪物基本无法反击

3. **速度提升（Speed）** *(给玩家)*
   - 为了补偿玩家也会受到一定影响
   - 给予玩家较弱的速度提升

## 视觉效果

- **粒子效果**：有冰冻附魔时，效果云使用**雪花粒子**（白色），而不是默认的治疗粒子（绿色）
- **效果图标**：怪物和玩家都会显示缓慢、挖掘疲劳和速度提升的状态图标

## 技术实现

### 核心代码位置

`/workspace/src/main/java/me/noramibu/mixin/HappyGhastEntityMixin.java`

#### 主要方法

1. **`spawnEffectCloud()`** - 生成效果云
   ```java
   // 检查冰冻附魔等级
   int freezingLevel = EnchantmentHelper.getEnchantmentLevel(
       ghast, 
       FireballEnchantment.FREEZING
   );
   
   // 根据附魔选择粒子效果
   if (freezingLevel > 0) {
       cloud.setParticleType(ParticleTypes.SNOWFLAKE);
   } else {
       cloud.setParticleType(ParticleTypes.HAPPY_VILLAGER);
   }
   
   // 添加冰冻效果
   if (freezingLevel > 0) {
       addFreezingEffect(cloud, freezingLevel);
   }
   ```

2. **`addFreezingEffect()`** - 添加冰冻状态效果
   ```java
   private void addFreezingEffect(AreaEffectCloudEntity cloud, int freezingLevel) {
       int duration;      // 持续时间（ticks）
       int amplifier;     // 缓慢强度
       
       // 根据等级设置参数
       switch (freezingLevel) {
           case 1: duration = 60;  amplifier = 4; break;  // 3秒，缓慢V
           case 2: duration = 100; amplifier = 6; break;  // 5秒，缓慢VII
           case 3: duration = 160; amplifier = 9; break;  // 8秒，缓慢X
       }
       
       // 添加缓慢效果
       cloud.addEffect(new StatusEffectInstance(
           StatusEffects.SLOWNESS, duration, amplifier, false, true
       ));
       
       // 添加挖掘疲劳效果
       cloud.addEffect(new StatusEffectInstance(
           StatusEffects.MINING_FATIGUE, duration, amplifier, false, true
       ));
       
       // 添加速度效果（给玩家补偿）
       cloud.addEffect(new StatusEffectInstance(
           StatusEffects.SPEED, duration, amplifier / 2, false, true
       ));
   }
   ```

### 与其他附魔的配合

- **持久附魔（Duration）**：不直接影响冰冻时长，但会延长效果云本身的持续时间
- **连射附魔（Multishot）**：每个火球的效果云都会有冰冻效果
- 冰冻效果是在效果云生成时添加的，与火球发射方式无关

## 获取附魔书

目前需要通过创造模式获取冰冻附魔书：

```
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"freezing",level:1}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"freezing",level:2}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"freezing",level:3}
```

*(未来可能会添加生存模式的合成配方)*

## 战术应用

### 控场效果

- **群体控制**：冰冻效果对进入效果云的所有怪物生效，可以有效控制大群敌人
- **安全撤退**：冰冻怪物可以为玩家争取宝贵的撤退时间
- **近战辅助**：冻住的怪物无法移动和攻击，玩家可以安全地近战击杀

### 最佳配合

1. **冰冻 + 持久**：延长效果云时间，让更多怪物进入并被冻结
2. **冰冻 + 连射**：多个冰冻云覆盖更大区域，形成"冰墙"
3. **3级冰冻**：8秒超长冻结时间，足够击杀大多数怪物

### 注意事项

⚠️ **玩家也会受到一定影响**：
- 效果云的效果会对所有实体生效，包括玩家
- 不过玩家会获得额外的速度提升来部分抵消
- 建议穿戴有速度或抗性的装备

⚠️ **与其他效果的交互**：
- 效果云同时包含伤害、治疗、速度和冰冻效果
- 所有效果会同时作用于进入云的实体

## 测试指南

### 测试步骤

1. **准备工作**
   ```
   # 获取3级快乐恶魂（效果云功能解锁）
   # 获取冰冻附魔书（1/2/3级）
   /give @p chest-on-ghast:enchanted_fireball_book{enchantment:"freezing",level:3}
   ```

2. **装备附魔**
   - 右键点击快乐恶魂打开GUI
   - 点击"编辑附魔"按钮
   - 将冰冻附魔书拖入附魔槽位

3. **测试冰冻效果**
   - 骑乘快乐恶魂或等待自动攻击
   - 向怪物发射火球
   - 观察效果云生成时的**雪花粒子**（白色）
   - 确认怪物进入云后被冻结，无法移动

4. **测试不同等级**
   - 测试1/2/3级的冻结时长差异
   - 观察怪物的状态图标（缓慢、挖掘疲劳）

5. **测试配合**
   - 同时装备冰冻和持久附魔
   - 同时装备冰冻和连射附魔
   - 观察效果叠加情况

### 预期结果

✅ **成功标志**：
- 效果云显示白色雪花粒子（而非绿色）
- 怪物进入云后显示缓慢和挖掘疲劳图标
- 怪物基本无法移动和攻击
- 等级越高，冻结时间越长（3秒/5秒/8秒）

## 版本历史

- **v1.0.4** (2025-11-09)
  - 实现冰冻附魔基础功能
  - 支持3个等级
  - 添加雪花粒子视觉效果
  - 组合缓慢和挖掘疲劳效果

## 已知问题与限制

1. **玩家影响**：玩家也会受到部分减速影响，虽然有速度补偿
2. **不死生物**：对某些特殊怪物（如末影人）可能效果不明显
3. **穿戴装备的怪物**：穿戴有速度或抗性装备的怪物可能受影响较小

## 相关文档

- [附魔系统总览](./ENCHANTMENT_SYSTEM_README.md)
- [连射附魔](./MULTISHOT_ENCHANTMENT_README.md)
- [持久附魔](./DURATION_ENCHANTMENT_README.md)
- [效果云系统](./EFFECT_CLOUD_README.md)
