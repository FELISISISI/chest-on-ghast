# 魅惑附魔 (Charm Enchantment)

## 概述

魅惑附魔是快乐恶魂火球附魔系统中最具战术性的附魔效果之一。当快乐恶魂的火球生成效果云时，魅惑附魔会让效果云范围内的怪物互相攻击，形成混乱的战场，让敌人自相残杀。

## 附魔效果

魅惑附魔会让效果云范围内的敌对生物每0.5秒（10 ticks）对彼此造成伤害，触发怪物的反击机制，形成混战局面。

### 等级效果

| 等级 | 互相伤害量 | 效果描述 |
|------|-----------|---------|
| I    | 2.0 (1颗心) | 基础魅惑，造成轻微伤害 |
| II   | 4.0 (2颗心) | 中级魅惑，造成中等伤害 |
| III  | 6.0 (3颗心) | 高级魅惑，造成严重伤害 |

### 工作原理

1. **范围检测**：每0.5秒，系统检查效果云范围内的所有敌对生物
2. **随机匹配**：为每个怪物随机选择范围内的另一个怪物作为目标
3. **伤害触发**：使用`mobAttack`伤害源让怪物对目标造成伤害
4. **反击机制**：被攻击的怪物会将攻击者设为目标，开始反击
5. **连锁反应**：怪物之间形成混战，持续到效果云消失或怪物死亡

### 特殊机制

- **最少2只怪物**：范围内至少需要2只怪物才会触发魅惑效果
- **仅对敌对生物有效**：只影响`HostileEntity`类型的怪物
- **伤害来源真实**：怪物受到的伤害来源是其他怪物，不是玩家
- **持续触发**：效果云存在期间，每0.5秒触发一次

## 视觉效果

- **粒子效果**：有魅惑附魔时，效果云使用**女巫粒子**（紫色魔法效果），而不是默认的治疗粒子（绿色）或冰冻粒子（白色）
- **攻击粒子**：每次怪物互相攻击时，会在攻击者头顶显示**愤怒粒子**（红色烟雾）
- **优先级**：魅惑粒子优先于冰冻粒子显示

## 技术实现

### 核心代码位置

`/workspace/src/main/java/me/noramibu/mixin/HappyGhastEntityMixin.java`

#### 主要字段

```java
// 用于追踪魅惑效果云（存储效果云ID和魅惑等级）
private final Map<Integer, Integer> charmClouds = new HashMap<>();

// 魅惑效果的处理间隔（ticks）
private int charmTickCounter = 0;
```

#### 主要方法

1. **`spawnEffectCloud()`** - 生成效果云并标记魅惑

```java
// 检查魅惑附魔（优先显示）
int charmLevel = EnchantmentHelper.getEnchantmentLevel(
    ghast, 
    FireballEnchantment.CHARM
);

// 设置紫色魔法粒子
if (charmLevel > 0) {
    cloud.setParticleType(ParticleTypes.WITCH);
}

// 追踪魅惑效果云
if (charmLevel > 0) {
    world.spawnEntity(cloud);
    charmClouds.put(cloud.getId(), charmLevel);
    return;  // 魅惑云不需要治疗效果
}
```

2. **`onTick()`** - 定期处理魅惑效果云

```java
// 处理魅惑效果云（每10 ticks检查一次）
charmTickCounter++;
if (charmTickCounter >= 10) {
    processCharmClouds(ghast);
    charmTickCounter = 0;
}
```

3. **`processCharmClouds()`** - 遍历所有魅惑效果云

```java
private void processCharmClouds(HappyGhastEntity ghast) {
    if (charmClouds.isEmpty()) return;
    
    // 检查每个追踪的效果云
    Iterator<Map.Entry<Integer, Integer>> iterator = charmClouds.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry<Integer, Integer> entry = iterator.next();
        int cloudId = entry.getKey();
        int charmLevel = entry.getValue();
        
        // 获取效果云实体
        Entity entity = ghast.getEntityWorld().getEntityById(cloudId);
        
        if (entity instanceof AreaEffectCloudEntity cloud) {
            // 应用魅惑效果
            applyCharmEffect(ghast.getEntityWorld(), cloud, charmLevel);
        } else {
            // 效果云消失，移除追踪
            iterator.remove();
        }
    }
}
```

4. **`applyCharmEffect()`** - 核心逻辑：让怪物互相攻击

```java
private void applyCharmEffect(World world, AreaEffectCloudEntity cloud, int charmLevel) {
    // 获取效果云范围内的所有敌对生物
    Box searchBox = new Box(
        cloudPos.x - radius, cloudPos.y - radius, cloudPos.z - radius,
        cloudPos.x + radius, cloudPos.y + radius, cloudPos.z + radius
    );
    
    List<HostileEntity> hostiles = world.getEntitiesByClass(
        HostileEntity.class, searchBox,
        entity -> entity.isAlive() && !entity.isRemoved()
    );
    
    // 至少需要2只怪物
    if (hostiles.size() >= 2) {
        for (HostileEntity attacker : hostiles) {
            // 随机选择另一个怪物作为目标
            List<HostileEntity> potentialTargets = new ArrayList<>(hostiles);
            potentialTargets.remove(attacker);
            
            if (!potentialTargets.isEmpty()) {
                HostileEntity target = potentialTargets.get(
                    world.getRandom().nextInt(potentialTargets.size())
                );
                
                // 造成伤害（使用mobAttack伤害源）
                target.damage(
                    serverWorld,
                    world.getDamageSources().mobAttack(attacker),
                    damageAmount
                );
                
                // 显示愤怒粒子
                serverWorld.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    attacker.getX(), attacker.getY() + attacker.getHeight() / 2, attacker.getZ(),
                    3, 0.3, 0.3, 0.3, 0.0
                );
            }
        }
    }
}
```

### 与其他附魔的配合

- **持久附魔（Duration）**：延长效果云时间，让怪物持续混战更久
- **连射附魔（Multishot）**：多个魅惑云覆盖更大区域，控制更多怪物
- **冰冻附魔（Freezing）**：不推荐同时使用，因为冰冻会让怪物无法移动，与魅惑的混战效果冲突

### 重要注意事项

⚠️ **魅惑云不包含治疗效果**：
- 为了平衡性，魅惑效果云不会给玩家恢复生命和速度
- 只包含对怪物的瞬间伤害和魅惑逻辑

⚠️ **效果云优先级**：
- 如果同时有魅惑和冰冻附魔，粒子会显示为紫色（魅惑优先）
- 但冰冻效果仍然会应用（怪物被减速）
- 这种组合实际上会降低魅惑效果（怪物无法追击）

## 获取附魔书

目前需要通过创造模式获取魅惑附魔书：

```
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"charm",level:1}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"charm",level:2}
/give @s chest-on-ghast:enchanted_fireball_book{enchantment:"charm",level:3}
```

*(未来可能会添加生存模式的合成配方)*

## 战术应用

### 群体控制大师

- **分散敌人注意**：怪物会攻击彼此，而不是玩家
- **减少承受伤害**：玩家可以躲在一旁，看怪物自相残杀
- **清理大群怪物**：在怪物聚集的地方使用，让它们互相削弱

### 最佳使用场景

1. **地牢探险**：在怪物刷怪笼附近使用，让新生成的怪物互相攻击
2. **夜晚防守**：在家门口使用，让夜晚的怪物群自相残杀
3. **要塞突袭**：在要塞或地下城中使用，让大量怪物内斗
4. **末地龙战**：对付末影人群体效果显著

### 最佳配合

1. **魅惑 + 持久（推荐）**：延长混战时间，让怪物打得更久
2. **魅惑 + 连射（推荐）**：多点开花，覆盖整个战场
3. **魅惑 + 冰冻（不推荐）**：冰冻会阻止怪物追击，降低魅惑效果
4. **3级魅惑**：每次攻击3颗心的伤害，快速消耗怪物血量

### 战术技巧

💡 **引导怪物进入云**：
- 魅惑云不会主动吸引怪物
- 需要将火球打到怪物聚集的地方
- 或者用玩家引导怪物进入云的范围

💡 **等待最佳时机**：
- 在怪物数量较多时使用效果最佳
- 至少需要2只怪物才会触发魅惑

💡 **保持安全距离**：
- 魅惑云不会攻击玩家
- 但混战中的怪物仍可能误伤玩家
- 建议保持一定距离观察

### 注意事项

⚠️ **对BOSS效果有限**：
- 凋灵、末影龙等BOSS怪物可能不会被魅惑
- 但可以用来清理BOSS周围的小怪

⚠️ **友军伤害**：
- 如果玩家的宠物（如驯服的狼）进入魅惑云，也可能被攻击
- 建议让宠物远离魅惑效果云

⚠️ **效果云寿命**：
- 效果云会随时间缩小并消失
- 持久附魔可以延长魅惑时间

## 测试指南

### 测试步骤

1. **准备工作**
   ```
   # 获取3级快乐恶魂（效果云功能解锁）
   # 获取魅惑附魔书（1/2/3级）
   /give @p chest-on-ghast:enchanted_fireball_book{enchantment:"charm",level:3}
   
   # 生成测试怪物（至少2只）
   /summon minecraft:zombie ~ ~ ~
   /summon minecraft:skeleton ~3 ~ ~
   ```

2. **装备附魔**
   - 右键点击快乐恶魂打开GUI
   - 点击"编辑附魔"按钮
   - 将魅惑附魔书拖入附魔槽位

3. **测试魅惑效果**
   - 骑乘快乐恶魂或等待自动攻击
   - 向怪物群发射火球
   - 观察效果云生成时的**紫色魔法粒子**
   - 确认怪物进入云后开始互相攻击

4. **观察战斗细节**
   - 查看怪物头顶的愤怒粒子（红色烟雾）
   - 观察怪物的血量变化
   - 确认怪物会追击攻击它们的其他怪物

5. **测试不同等级**
   - 测试1/2/3级的伤害差异
   - 1级：每次1颗心，较慢
   - 2级：每次2颗心，中等
   - 3级：每次3颗心，快速

6. **测试附魔配合**
   - 同时装备魅惑和持久附魔
   - 观察混战持续时间是否延长

### 预期结果

✅ **成功标志**：
- 效果云显示紫色魔法粒子（女巫效果）
- 怪物进入云后开始互相攻击
- 怪物头顶显示愤怒粒子
- 怪物血量持续下降
- 怪物会追击攻击它们的其他怪物

### 常见问题

**Q: 为什么怪物没有互相攻击？**
- A: 确保范围内至少有2只怪物
- A: 检查效果云是否正确生成（紫色粒子）
- A: 等待0.5秒（10 ticks），效果不是瞬间的

**Q: 玩家会受到魅惑影响吗？**
- A: 不会，魅惑只对`HostileEntity`（敌对生物）有效

**Q: 魅惑效果会持续多久？**
- A: 持续整个效果云的寿命（由快乐恶魂等级和持久附魔决定）

## 版本历史

- **v1.0.4** (2025-11-09)
  - 实现魅惑附魔基础功能
  - 支持3个等级的伤害量
  - 添加紫色魔法粒子视觉效果
  - 实现每0.5秒的互相攻击逻辑
  - 添加愤怒粒子效果

## 已知问题与限制

1. **需要多只怪物**：至少2只怪物才会触发，单只怪物无效
2. **范围限制**：只影响效果云范围内的怪物，范围外的怪物不受影响
3. **伤害来源**：由于使用`mobAttack`伤害源，某些特殊怪物可能不会反击
4. **与冰冻冲突**：同时使用魅惑和冰冻会降低效果（怪物被冻结无法追击）

## 相关文档

- [附魔系统总览](./ENCHANTMENT_SYSTEM_README.md)
- [连射附魔](./MULTISHOT_ENCHANTMENT_README.md)
- [持久附魔](./DURATION_ENCHANTMENT_README.md)
- [冰冻附魔](./FREEZING_ENCHANTMENT_README.md)
- [效果云系统](./EFFECT_CLOUD_README.md)

## 开发者备注

魅惑附魔是最复杂的附魔实现之一，涉及到：
- 实体追踪系统（追踪效果云）
- 定期tick处理（每10 ticks检查一次）
- 范围检测和实体筛选
- 伤害源正确设置（让怪物相信伤害来自其他怪物）
- 粒子效果同步

这个实现展示了Minecraft Fabric Mod开发中的多种技术：
- Mixin注入
- 实体操作
- 伤害系统
- 粒子效果
- 定时逻辑
