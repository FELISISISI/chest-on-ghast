# 滑稽变形附魔 (Hilarious Polymorph Enchantment)

## 概述

滑稽变形是快乐恶魂火球附魔系统中**最搞笑、最有娱乐性**的附魔效果！它直接改变了怪物的本质——将凶恶的敌对生物变成无害的、滑稽的被动动物。

想象一下：一个巨大的末影人被变成一只惊慌失措的小鸡，这种反差萌会带来巨大的欢乐！这不仅是最有效的硬控场方式，也是最有趣的战斗体验。

## 附魔效果

### 等级效果

| 等级 | 变形概率 | 效果描述 |
|------|---------|---------|
| I    | 33%     | 低概率变形，适合娱乐 |
| II   | 66%     | 中等概率，实用性显著 |
| III  | 100%    | 必定变形，完全控场 |

### 变形对象（随机选择）

变形后的被动生物种类（5种）：

1. **🐔 鸡 (Chicken)**
   - 特点：最小最可爱
   - 反差：末影人变鸡最搞笑
   - 会到处乱跑，发出"咯咯"声

2. **🐰 兔子 (Rabbit)**
   - 特点：蹦蹦跳跳
   - 反差：爬行者变兔子最萌
   - 跳跃移动，非常灵活

3. **🐷 猪 (Pig)**
   - 特点：笨拙可爱
   - 反差：骷髅变猪很好玩
   - 可以用胡萝卜钓竿骑乘

4. **🐑 羊 (Sheep)**
   - 特点：温顺毛茸茸
   - 反差：僵尸变羊很治愈
   - 可以剪羊毛获得羊毛

5. **🐮 牛 (Cow)**
   - 特点：体型大，憨厚
   - 反差：凋灵骷髅变牛巨大反差
   - 可以挤奶

## 核心机制

### 1. 变形触发

```
火球击中 → 生成效果云 → 效果云范围检测 → 概率判定 → 执行变形
```

### 2. 变形过程

1. **保存原怪物信息**：
   - 位置坐标
   - 朝向（yaw, pitch）
   - 自定义名字（如果有）

2. **随机选择被动生物**：
   - 5种被动生物随机
   - 每种概率均等（20%）

3. **创建新实体**：
   - 在原位置生成被动生物
   - 继承自定义名字
   - 设置相同朝向

4. **移除原怪物**：
   - 调用`discard()`删除
   - 不会掉落战利品
   - 不会产生经验

5. **特效和音效**：
   - 华丽的变形粒子（4层）
   - 高音调的魔法音效

### 3. 防止重复变形

- 使用Set追踪已变形的实体ID
- 每个怪物只能被变形一次
- 定期清理记录（每10秒）

## 视觉效果

### 四层粒子系统

1. **不死图腾粒子** (TOTEM_OF_UNDYING - 金色)
   - 30个粒子
   - 爆炸式扩散
   - 最显眼的视觉标志

2. **爆炸粒子** (EXPLOSION - 白色)
   - 5个粒子
   - 白色闪光效果
   - 模拟变形冲击波

3. **快乐村民粒子** (HAPPY_VILLAGER - 绿色)
   - 20个粒子
   - 绿色爱心
   - 表达欢乐愉悦

4. **传送门粒子** (PORTAL - 紫色)
   - 50个粒子
   - 紫色烟雾
   - 模拟空间扭曲

### 音效

- **音效类型**：`ENTITY_ILLUSIONER_MIRROR_MOVE`
- **音调**：1.5（高音调，更滑稽）
- **音量**：1.0（正常）
- **音效分类**：HOSTILE

### 效果云粒子

- **粒子类型**：TOTEM_OF_UNDYING（金色）
- **视觉风格**：欢乐、卡通、魔法

## 技术实现

### 核心代码位置

`/workspace/src/main/java/me/noramibu/mixin/HappyGhastEntityMixin.java`

#### 主要字段

```java
// 用于追踪变形效果云（存储效果云ID和变形等级）
private final Map<Integer, Integer> polymorphClouds = new HashMap<>();

// 变形效果的处理间隔（每5 ticks）
private int polymorphTickCounter = 0;

// 用于追踪已变形的实体ID（防止重复变形）
private final Set<Integer> polymorphedEntities = new HashSet<>();
```

#### 主要方法

1. **`spawnEffectCloud()`** - 检测并生成变形效果云

```java
// 检查滑稽变形附魔
int polymorphLevel = EnchantmentHelper.getEnchantmentLevel(
    ghast, 
    FireballEnchantment.POLYMORPH
);

if (polymorphLevel > 0) {
    // 使用彩色爆炸粒子（欢乐感）
    cloud.setParticleType(ParticleTypes.TOTEM_OF_UNDYING);
    
    // 变形效果不需要任何状态效果，只需要变形逻辑
    world.spawnEntity(cloud);
    polymorphClouds.put(cloud.getId(), polymorphLevel);
    return;
}
```

2. **`processPolymorphClouds()`** - 每5 ticks处理所有变形云

```java
// 处理滑稽变形（每5 ticks检查一次）
polymorphTickCounter++;
if (polymorphTickCounter >= 5) {
    processPolymorphClouds(ghast);
    polymorphTickCounter = 0;
}
```

3. **`applyPolymorphEffect()`** - 应用变形效果

```java
// 根据附魔等级确定变形概率
double polymorphChance;
switch (polymorphLevel) {
    case 1: polymorphChance = 0.33; break;  // 33%
    case 2: polymorphChance = 0.66; break;  // 66%
    case 3: polymorphChance = 1.0; break;   // 100%
}

// 对每个怪物尝试变形
for (HostileEntity hostile : hostiles) {
    if (!polymorphedEntities.contains(hostile.getId())) {
        if (random.nextDouble() < polymorphChance) {
            polymorphHostileToPassive(world, hostile);
            polymorphedEntities.add(hostile.getId());
        }
    }
}
```

4. **`polymorphHostileToPassive()`** - 核心变形逻辑

```java
// 随机选择被动生物类型
EntityType<?> passiveType;
int choice = random.nextInt(5);

switch (choice) {
    case 0: passiveType = EntityType.CHICKEN; break;
    case 1: passiveType = EntityType.RABBIT; break;
    case 2: passiveType = EntityType.PIG; break;
    case 3: passiveType = EntityType.SHEEP; break;
    case 4: passiveType = EntityType.COW; break;
}

// 创建新实体
Entity passiveEntity = passiveType.create(world, SpawnReason.MOB_SUMMONED);
passiveEntity.refreshPositionAndAngles(x, y, z, yaw, pitch);

// 继承名字
if (hostile.hasCustomName()) {
    passiveEntity.setCustomName(hostile.getCustomName());
    passiveEntity.setCustomNameVisible(hostile.isCustomNameVisible());
}

// 生成新实体，移除旧怪物
world.spawnEntity(passiveEntity);
hostile.discard();

// 特效
spawnPolymorphParticles(world, x, y, z);
```

5. **`spawnPolymorphParticles()`** - 生成华丽粒子

### 性能优化

1. **实体限制**：
   - 最多处理10个怪物/次
   - 防止大量怪物导致卡顿

2. **更新频率**：
   - 每5 ticks检查一次
   - 平衡性能和效果

3. **内存管理**：
   - polymorphClouds最多20条
   - polymorphedEntities最多100条
   - 每200 ticks清理一次

4. **防止重复**：
   - Set追踪已变形实体
   - 避免重复变形消耗

## 获取附魔书

通过创造模式命令获取滑稽变形附魔书：

```
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"polymorph",level:1}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"polymorph",level:2}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"polymorph",level:3}
```

*(未来可能会添加生存模式的合成配方)*

## 战术应用

### 终极硬控

滑稽变形是**最强的控制附魔**：

1. **完全无害化**：
   - 怪物变成被动生物
   - 不会攻击玩家
   - 不会追踪玩家

2. **永久效果**：
   - 变形后不会恢复
   - 相当于直接移除威胁
   - 比冰冻和魅惑都强

3. **额外收益**：
   - 变成的动物可以繁殖
   - 可以获得资源（羊毛、牛奶等）
   - 变成"农场"

### 最佳使用场景

| 场景 | 评分 | 说明 |
|------|------|------|
| 地牢探险 | ⭐⭐⭐⭐⭐ | 永久清除威胁 |
| BOSS战 | ⭐⭐ | BOSS可能免疫 |
| 防守战 | ⭐⭐⭐⭐⭐ | 将入侵者变无害 |
| 娱乐PVP | ⭐⭐⭐⭐⭐ | 超级搞笑 |
| 刷怪场 | ⭐⭐ | 会影响经验获取 |

### 战术技巧

💡 **娱乐优先**：
- 这个附魔主要用于娱乐
- 看末影人变鸡的反差萌
- 适合朋友一起玩时搞笑

💡 **不掉落战利品**：
- 变形后怪物不掉落物品
- 不适合刷怪获取资源
- 更适合防守和探险

💡 **可以养殖**：
- 变形后的动物可以繁殖
- 僵尸猪 → 猪 → 繁殖 → 猪排
- 意外的额外收益

💡 **3级最实用**：
- 100%概率保证变形
- 适合清场和防守
- 1-2级更有随机性和娱乐性

### 搞笑组合

1. **变形 + 连射** ⭐⭐⭐⭐⭐
   - 多个变形云同时作用
   - 大片怪物瞬间变动物
   - 最搞笑的组合

2. **变形 + 持久**⭐⭐⭐⭐
   - 效果云持续时间长
   - 后续进入的怪物也会变形
   - 适合防守要塞

3. 变形 + 引力 ⭐⭐⭐
   - 先聚拢再变形
   - 一堆动物在中心
   - 视觉效果搞笑

## 注意事项

⚠️ **不掉落物品**：
- 变形的怪物不会掉落战利品
- 不会获得经验值
- 纯控制效果，无收益

⚠️ **不可逆**：
- 变形是永久的
- 被动生物不会变回怪物
- 请谨慎使用

⚠️ **BOSS可能免疫**：
- 某些BOSS（末影龙、凋灵）可能无法变形
- 这是Minecraft原版机制
- 小怪都可以变形

⚠️ **占用实体数量**：
- 变形后的动物会占用实体数量
- 大量变形可能影响性能
- 建议定期清理

⚠️ **名字保留**：
- 如果怪物有自定义名字
- 变形后的动物会继承
- 可能导致奇怪的现象（"僵尸王"变成一只猪）

## 趣味场景

### 🎪 最搞笑的变形组合

1. **末影人 → 鸡**
   - 从3格高到0.7格高
   - 巨大反差萌
   - 满分搞笑 ⭐⭐⭐⭐⭐

2. **爬行者 → 兔子**
   - 从爆炸怪到蹦蹦跳
   - 威胁感瞬间消失
   - 很可爱 ⭐⭐⭐⭐

3. **凋灵骷髅 → 牛**
   - 从暗黑战士到农场动物
   - 强烈违和感
   - 超级好笑 ⭐⭐⭐⭐⭐

4. **骷髅射手 → 羊**
   - 从远程狙击到温顺羊咩咩
   - 可以剪羊毛了
   - 实用且有趣 ⭐⭐⭐⭐

5. **僵尸群 → 猪群**
   - 从丧尸潮到养猪场
   - 场面极其滑稽
   - 可以繁殖 ⭐⭐⭐⭐

### 🎬 趣味应用

**场景1：末地龙战**
```
1. 带上3级变形附魔快乐恶魂
2. 对着末影人群发射火球
3. 末影人瞬间变成鸡和兔子
4. 龙蛋周围全是小动物，超级搞笑
```

**场景2：地牢清场**
```
1. 进入刷怪笼房间
2. 发射变形火球
3. 刷怪笼继续生成僵尸
4. 但全变成猪，变成"养猪场"
5. 可以繁殖获得猪排
```

**场景3：PVP娱乐**
```
1. 与朋友对战
2. 等对方召唤宠物（狼、猫等）
3. 发射变形火球
4. 对方的"精英战队"变成农场动物
5. 场面失控，笑到崩溃
```

## 测试指南

### 测试步骤

1. **准备工作**
   ```
   # 获取3级快乐恶魂
   # 获取滑稽变形附魔书（3级推荐）
   /give @p chest-on-ghast:enchanted_fireball_book{enchantment:"polymorph",level:3}
   
   # 生成各种怪物（测试不同变形效果）
   /summon minecraft:zombie ~ ~ ~ {CustomName:'{"text":"僵尸王"}'}
   /summon minecraft:skeleton ~3 ~ ~
   /summon minecraft:enderman ~-3 ~ ~
   /summon minecraft:creeper ~ ~ ~3
   ```

2. **装备附魔**
   - 打开快乐恶魂GUI
   - 装备滑稽变形附魔书

3. **测试基础变形**
   - 向怪物发射火球
   - 观察金色粒子效果（不死图腾）
   - 确认怪物变成被动生物

4. **测试不同等级**
   - 1级：33%概率，需要多试几次
   - 2级：66%概率，大部分会变
   - 3级：100%概率，全部变形

5. **测试有趣场景**
   - 末影人变鸡（最搞笑）
   - 观察变形粒子（4层）
   - 听变形音效（高音调）

6. **测试名字继承**
   - 生成有名字的怪物
   - 变形后查看是否保留名字

### 预期结果

✅ **成功标志**：
- 火球击中后生成金色效果云
- 怪物变成随机被动生物（鸡/兔/猪/羊/牛）
- 华丽的变形粒子（金色+白色+绿色+紫色）
- 高音调魔法音效
- 原怪物消失，新动物出现在同位置

## 版本历史

- **v1.0.5** (2025-11-09)
  - 实现滑稽变形附魔基础功能
  - 支持3个等级（33%/66%/100%概率）
  - 5种随机被动生物（鸡/兔/猪/羊/牛）
  - 华丽4层粒子效果
  - 名字继承机制
  - 防重复变形逻辑
  - 性能优化（最多10怪物，定期清理）

## 已知问题与限制

1. **不掉落战利品**：
   - 这是设计决定
   - 变形怪物不掉落物品和经验
   - 纯控制/娱乐效果

2. **不可逆**：
   - 变形是永久的
   - 无法变回原怪物
   - 可能未来添加"还原"附魔？

3. **BOSS限制**：
   - 某些BOSS可能免疫
   - 末影龙、凋灵可能无法变形
   - 小BOSS（如凋灵骷髅）可以变形

4. **实体数量**：
   - 大量变形会增加实体数
   - 可能影响性能
   - 建议不要滥用

5. **随机性**：
   - 无法指定变形目标
   - 完全随机5选1
   - 增加趣味性但降低可控性

## 相关文档

- [附魔系统总览](./ENCHANTMENT_SYSTEM_README.md)
- [连射附魔](./MULTISHOT_ENCHANTMENT_README.md)（已完成）
- [持久附魔](./DURATION_ENCHANTMENT_README.md)（已完成）
- [冰冻附魔](./FREEZING_ENCHANTMENT_README.md)（已完成）
- [魅惑附魔](./CHARM_ENCHANTMENT_README.md)（已完成）
- [引力奇点附魔](./GRAVITY_SINGULARITY_ENCHANTMENT_README.md)（已完成）
- [效果云系统](./EFFECT_CLOUD_README.md)

## 开发者备注

滑稽变形是最具娱乐性的附魔实现：

### 技术亮点

1. **实体替换**：
   - 完整的实体创建和删除流程
   - 保留位置、朝向、名字
   - 无缝转换

2. **防重复机制**：
   - Set追踪已变形实体
   - 防止同一怪物被反复变形
   - 内存泄漏防护

3. **随机系统**：
   - 5种被动生物随机
   - 概率系统（33%/66%/100%）
   - 增加趣味性

4. **粒子系统**：
   - 4层粒子叠加
   - 华丽视觉效果
   - 符合卡通风格

### 设计哲学

**"用魔法将战斗变成喜剧"**

滑稽变形完美诠释了Minecraft的轻松愉快风格。它不追求效率或伤害，而是追求欢乐和创意。这就是沙盒游戏的魅力——你可以把末影人变成鸡！

### 代码统计

- **新增行数**：约270行
- **Mixin总行数**：1525行
- **新增方法**：
  - `processPolymorphClouds()` - 处理变形云
  - `applyPolymorphEffect()` - 应用变形
  - `polymorphHostileToPassive()` - 核心变形
  - `spawnPolymorphParticles()` - 粒子效果

---

**创意来源**：

滑稽变形的灵感来自：
- 《炉石传说》的"变羊术"
- 《魔兽世界》的"变形术"
- 各种游戏中的"Polymorph"魔法

但我们做得更有趣——变成**5种随机动物**，而不是固定的一种！

---

**致谢**：

感谢用户提出这个充满想象力和欢乐的附魔创意！滑稽变形不仅技术上有趣，而且在游戏中极具娱乐价值。

看到僵尸王变成一只叫"僵尸王"的小鸡，真的会笑出声！😂🐔

---

**Minecraft 快乐恶魂 Mod - 让战斗变得有趣！**
