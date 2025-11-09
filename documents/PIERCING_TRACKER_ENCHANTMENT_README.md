# 穿透追踪附魔 (Piercing Tracker Enchantment)

## ⚠️ 开发状态

**当前状态：部分实现（附魔书可装备，但效果暂未生效）**

穿透追踪附魔由于技术复杂度较高，需要深度修改`FireballEntity`的碰撞检测和飞行逻辑，目前仅完成了附魔书系统集成，实际效果尚未实现。

## 概述

穿透追踪附魔是快乐恶魂火球附魔系统中技术难度最高的附魔之一。设计目标是让火球在击中第一个目标后不消失，而是继续飞行并自动追踪下一个最近的敌人，实现"一箭多雕"的效果。

## 设计效果

### 等级效果（计划）

| 等级 | 可击中目标数 | 效果描述 |
|------|------------|---------|
| I    | 2个目标 | 火球穿透第一个目标，追踪第二个 |
| II   | 3个目标 | 火球连续穿透并追踪3个目标 |
| III  | 5个目标 | 火球连续穿透并追踪5个目标 |

### 工作原理（计划）

1. **击中检测**：火球击中第一个敌对生物
2. **伤害计算**：对目标造成固定伤害（6点）
3. **目标记录**：记录已击中的目标UUID，避免重复攻击
4. **寻找下一个**：在16格范围内搜索未被击中的最近敌人
5. **追踪转向**：平滑转向下一个目标
6. **重复过程**：继续2-5步，直到达到最大目标数或无可用目标

## 技术挑战

### 为什么难以实现？

1. **实体行为修改**：
   - `FireballEntity`的碰撞处理是final方法，难以直接override
   - 需要使用Mixin注入到碰撞事件中
   - 需要阻止火球在击中后立即消失

2. **状态持久化**：
   - 需要追踪已击中的目标列表
   - 火球实体的NBT数据访问受限
   - 需要自定义数据存储方式

3. **追踪逻辑**：
   - 需要每tick更新火球方向
   - 目标搜索算法需要优化性能
   - 转向需要平滑，避免突兀

4. **API限制**：
   - Minecraft 1.21.9的API与旧版本差异巨大
   - 很多NBT和实体方法已改名或删除
   - 自定义实体注册机制复杂

### 尝试过的实现方案

#### 方案1：自定义实体（失败）

尝试创建`TrackingFireballEntity`继承`FireballEntity`：

```java
public class TrackingFireballEntity extends FireballEntity {
    private int maxTargets;
    private List<UUID> hitTargets;
    private LivingEntity currentTarget;
    // ...
}
```

**失败原因**：
- 构造函数签名不匹配
- 无法正确注册EntityType
- NBT方法writeNbt/readNbt在FireballEntity中不可访问
- API版本兼容性问题

#### 方案2：NBT数据标记（失败）

尝试在火球创建时写入NBT数据：

```java
NbtCompound nbt = new NbtCompound();
fireball.writeNbt(nbt);  // 方法不存在！
nbt.putInt("PiercingLevel", piercingLevel);
fireball.readNbt(nbt);  // 方法不存在！
```

**失败原因**：
- FireballEntity没有公开的writeNbt/readNbt方法
- 实体数据无法直接访问

#### 方案3：Mixin注入（计划中）

需要创建`FireballEntityMixin`来注入碰撞逻辑：

```java
@Mixin(FireballEntity.class)
public class FireballEntityMixin {
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onFireballHit(HitResult hitResult, CallbackInfo ci) {
        // 检查是否有穿透追踪标记
        // 如果有，不销毁火球，转向下一个目标
        // 如果达到最大目标数，允许正常爆炸
    }
}
```

**挑战**：
- 需要更深入了解FireballEntity的内部结构
- 如何在Mixin中存储状态（已击中目标列表）
- 如何实现平滑的追踪转向

## 当前实现

### 已完成部分

- ✅ 附魔枚举定义（`FireballEnchantment.PIERCING`）
- ✅ 附魔书物品创建
- ✅ GUI中附魔槽位显示
- ✅ 附魔数据存储和同步
- ✅ 附魔等级与目标数量映射（2/3/5）
- ✅ 翻译文本（中文/英文）

### 未完成部分

- ❌ 火球穿透逻辑（核心功能）
- ❌ 目标追踪算法
- ❌ 伤害计算和目标记录
- ❌ 火球状态持久化

## 获取附魔书

虽然效果尚未实现，但可以获取并装备附魔书：

```
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"piercing",level:1}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"piercing",level:2}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"piercing",level:3}
```

装备后，在GUI中可以看到附魔显示，但火球不会有穿透追踪效果。

## 替代方案

在穿透追踪附魔完全实现之前，可以使用以下组合来达到类似效果：

### 推荐组合

1. **连射 + 魅惑**：
   - 多个火球覆盖不同区域
   - 魅惑让怪物自相残杀
   - 间接达到"多目标攻击"效果

2. **连射 + 冰冻**：
   - 多个冰冻云控制大片区域
   - 怪物被冻结后无法逃脱
   - 便于逐个击杀

## 未来计划

### 版本路线图

- **v1.1.0（计划）**：
  - 研究FireballEntity Mixin注入
  - 实现基础的碰撞拦截
  - 添加简单的目标记录

- **v1.2.0（计划）**：
  - 实现目标搜索算法
  - 添加追踪转向逻辑
  - 完整穿透追踪效果

- **v1.3.0（计划）**：
  - 优化性能
  - 添加粒子轨迹效果
  - 平衡性调整

### 社区贡献

如果你有Minecraft Fabric Mod开发经验，特别是熟悉：
- Mixin注入到原版实体
- 实体NBT数据存储
- 实体碰撞和追踪逻辑

欢迎贡献代码！这是一个很有挑战性的feature。

## 技术文档

### 代码位置

- 附魔定义：`/workspace/src/main/java/me/noramibu/enchantment/FireballEnchantment.java`
- 附魔辅助：`/workspace/src/main/java/me/noramibu/enchantment/EnchantmentHelper.java`
- 计划的Mixin：`/workspace/src/main/resources/chest-on-ghast.mixins.json`（需添加）

### 预留代码

```java
// EnchantmentHelper.java line 86-93
// 检查穿透追踪附魔（注意：此附魔功能需要额外的Mixin实现）
int piercingLevel = getEnchantmentLevel(ghast, FireballEnchantment.PIERCING);

// TODO: 穿透追踪附魔需要额外的FireballEntity Mixin来实现
// 目前暂时未完全实现，附魔书可以装备但效果不生效
if (piercingLevel > 0) {
    // 未来版本将实现：火球击中目标后不消失，转向下一个敌人
}
```

### 实现建议

对于想要实现这个功能的开发者，建议参考：

1. **Fabric Mixin文档**：
   - https://fabricmc.net/wiki/tutorial:mixin_introduction
   - https://github.com/SpongePowered/Mixin/wiki

2. **原版实体追踪**：
   - 研究`WitherSkullEntity`的追踪逻辑
   - 参考`HomeAttackGoal`的目标寻找算法

3. **数据存储**：
   - 使用AccessWidener访问private字段
   - 或者使用@Unique字段存储自定义数据

## 已知问题

1. **附魔书可装备但无效果**：这是正常的，核心功能尚未实现
2. **与其他附魔冲突**：穿透追踪不应与连射同时使用（设计上冲突）
3. **GUI显示正常**：附魔槽位、等级显示、翻译都已正确实现

## 相关文档

- [附魔系统总览](./ENCHANTMENT_SYSTEM_README.md)
- [连射附魔](./MULTISHOT_ENCHANTMENT_README.md)（已完成）
- [持久附魔](./DURATION_ENCHANTMENT_README.md)（已完成）
- [冰冻附魔](./FREEZING_ENCHANTMENT_README.md)（已完成）
- [魅惑附魔](./CHARM_ENCHANTMENT_README.md)（已完成）
- [效果云系统](./EFFECT_CLOUD_README.md)

## 开发者备注

穿透追踪附魔是这个mod最具挑战性的部分。它需要：
- 深入理解Minecraft实体系统
- 熟练使用Mixin注入技术
- 处理复杂的状态管理
- 优化性能（避免每tick大范围搜索）
- 确保网络同步（客户端/服务端）

如果你成功实现了这个功能，请考虑分享你的代码！这将是Fabric Mod开发的一个很好的学习案例。

---

**致歉声明**：

很抱歉未能在当前版本中完全实现穿透追踪附魔。这个功能比最初预期的要复杂得多，特别是Minecraft 1.21.9的API变化导致了很多兼容性问题。

我们承诺会在未来版本中继续努力实现这个功能。同时，其他4个附魔（连射、持久、冰冻、魅惑）都已完整实现并可正常使用，它们已经为快乐恶魂提供了强大且有趣的战斗能力。

感谢你的理解和支持！🙏
