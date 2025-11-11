# 快乐恶魂战斗系统配置指南

## 📋 配置文件位置

配置文件会在首次运行模组时自动生成：
```
.minecraft/config/chest-on-ghast.json
```

## 🎯 战斗参数配置

每个等级都有独立的战斗参数，完全可自定义：

### 配置项说明

| 参数名 | 类型 | 说明 | 示例值 |
|--------|------|------|--------|
| `fireballPower` | 整数 | 火球威力（爆炸强度） | 1-10 |
| `attackCooldown` | 整数 | 攻击冷却时间（游戏刻） | 15-100 |

**重要提示**：
- `attackCooldown` 单位是 **ticks（游戏刻）**
- **20 ticks = 1秒**
- 例如：60 ticks = 3秒，40 ticks = 2秒，15 ticks = 0.75秒

## 📝 完整配置示例

```json
{
  "levels": {
    "1": {
      "maxHealth": 20.0,
      "maxHunger": 100.0,
      "expToNextLevel": 100,
      "hungerDecayMultiplier": 1.0,
      "fireballPower": 1,
      "attackCooldown": 60
    },
    "2": {
      "maxHealth": 30.0,
      "maxHunger": 200.0,
      "expToNextLevel": 200,
      "hungerDecayMultiplier": 0.9,
      "fireballPower": 2,
      "attackCooldown": 50
    },
    "3": {
      "maxHealth": 45.0,
      "maxHunger": 400.0,
      "expToNextLevel": 350,
      "hungerDecayMultiplier": 0.81,
      "fireballPower": 3,
      "attackCooldown": 40
    },
    "4": {
      "maxHealth": 65.0,
      "maxHunger": 800.0,
      "expToNextLevel": 550,
      "hungerDecayMultiplier": 0.729,
      "fireballPower": 4,
      "attackCooldown": 30
    },
    "5": {
      "maxHealth": 90.0,
      "maxHunger": 1600.0,
      "expToNextLevel": 800,
      "hungerDecayMultiplier": 0.6561,
      "fireballPower": 5,
      "attackCooldown": 20
    },
    "6": {
      "maxHealth": 120.0,
      "maxHunger": 3200.0,
      "expToNextLevel": 0,
      "hungerDecayMultiplier": 0.59049,
      "fireballPower": 6,
      "attackCooldown": 15
    }
  },
  "foodConfig": {
    "snowballHunger": 50.0,
    "snowballExp": 10,
    "favoriteHunger": 80.0,
    "favoriteExp": 20,
    "defaultHunger": 12.0,
    "defaultExp": 5
  }
}
```

## 🎮 默认战斗配置

### 等级战斗力对比表

| 等级 | 火球威力 | 攻击冷却 | 实际攻速 | 战斗风格 |
|------|---------|----------|----------|---------|
| 1 | 1 | 60 ticks | 3.0秒/次 | 🔰 新手守护者 |
| 2 | 2 | 50 ticks | 2.5秒/次 | 🛡️ 可靠保镖 |
| 3 | 3 | 40 ticks | 2.0秒/次 | ⚔️ 战斗伙伴 |
| 4 | 4 | 30 ticks | 1.5秒/次 | 💪 强力战士 |
| 5 | 5 | 20 ticks | 1.0秒/次 | 🔥 精英守卫 |
| 6 | 6 | 15 ticks | 0.75秒/次 | 👑 传奇守护神 |

**DPS（每秒伤害）提升**：
- 等级1：威力1 / 3秒 = 0.33 DPS
- 等级3：威力3 / 2秒 = 1.5 DPS（4.5倍提升！）
- 等级6：威力6 / 0.75秒 = 8 DPS（24倍提升！）

## 🛠️ 自定义配置示例

### 示例1：超快速低威力（机关枪模式）
```json
"6": {
  "fireballPower": 2,
  "attackCooldown": 5
}
```
- 威力降低但攻速极快（每0.25秒一次）
- 适合快速清理小怪

### 示例2：超高威力慢速（重炮模式）
```json
"6": {
  "fireballPower": 10,
  "attackCooldown": 100
}
```
- 威力极高但攻速慢（每5秒一次）
- 适合对付强大的Boss

### 示例3：平衡模式
```json
"6": {
  "fireballPower": 4,
  "attackCooldown": 30
}
```
- 威力和攻速都很平衡
- 适合日常战斗

### 示例4：逐级递增模式
```json
{
  "1": { "fireballPower": 1, "attackCooldown": 80 },
  "2": { "fireballPower": 2, "attackCooldown": 70 },
  "3": { "fireballPower": 3, "attackCooldown": 60 },
  "4": { "fireballPower": 4, "attackCooldown": 50 },
  "5": { "fireballPower": 5, "attackCooldown": 40 },
  "6": { "fireballPower": 6, "attackCooldown": 30 }
}
```
- 每级稳定提升
- 成长曲线平滑

## ⚙️ 配置修改步骤

1. **定位配置文件**
   - 进入 `.minecraft/config/` 目录
   - 找到 `chest-on-ghast.json` 文件

2. **备份原配置**
   - 建议先复制一份原文件作为备份

3. **编辑配置**
   - 使用文本编辑器打开
   - 修改对应等级的 `fireballPower` 和 `attackCooldown`
   - 保存文件

4. **重启游戏**
   - 关闭并重新启动Minecraft
   - 新的配置会自动生效

## 💡 配置建议

### 火球威力（fireballPower）建议值

| 威力值 | 效果 | 适用场景 |
|--------|------|---------|
| 1-2 | 小爆炸 | 和平模式、装饰用 |
| 3-4 | 中等爆炸 | 正常游戏 |
| 5-6 | 大爆炸 | 困难模式 |
| 7-10 | 超强爆炸 | 极限挑战 |
| 10+ | 毁天灭地 | ⚠️ 慎用！可能破坏地形 |

### 攻击冷却（attackCooldown）建议值

| 冷却时间 | 等效秒数 | 攻击频率 | 适用场景 |
|---------|---------|---------|---------|
| 10 ticks | 0.5秒 | 极快 | 机关枪模式 |
| 20 ticks | 1.0秒 | 快速 | 激烈战斗 |
| 40 ticks | 2.0秒 | 正常 | 平衡游戏 |
| 60 ticks | 3.0秒 | 较慢 | 休闲模式 |
| 100 ticks | 5.0秒 | 很慢 | 重炮模式 |

## ⚠️ 注意事项

1. **JSON格式**
   - 请确保JSON格式正确
   - 注意逗号和括号的配对
   - 建议使用JSON验证工具检查

2. **数值范围**
   - `fireballPower`：建议1-10，过高可能破坏地形
   - `attackCooldown`：建议5-100，过低可能影响性能

3. **游戏平衡**
   - 过高的威力可能让游戏失去挑战性
   - 过快的攻速可能导致卡顿
   - 建议根据服务器情况调整

4. **配置生效**
   - 修改配置后需要重启游戏
   - 已存在的快乐恶魂会使用新配置
   - 不需要重新生成世界

## 🔧 故障排除

### 配置文件不生效
- 检查JSON格式是否正确
- 确认文件保存位置正确
- 尝试删除配置文件让其重新生成

### 火球威力过大
- 降低 `fireballPower` 值
- 建议保持在1-6范围内

### 攻击速度太快/太慢
- 调整 `attackCooldown` 值
- 记住：20 ticks = 1秒

## 📊 配置文件结构

```
chest-on-ghast.json
├── levels (等级配置)
│   ├── 1 (等级1配置)
│   │   ├── maxHealth (最大血量)
│   │   ├── maxHunger (最大饱食度)
│   │   ├── expToNextLevel (升级所需经验)
│   │   ├── hungerDecayMultiplier (饱食度消耗倍率)
│   │   ├── fireballPower (火球威力) ⭐
│   │   └── attackCooldown (攻击冷却) ⭐
│   ├── 2-6 (其他等级，结构相同)
│   └── ...
└── foodConfig (喂食配置)
    ├── snowballHunger
    ├── snowballExp
    ├── favoriteHunger
    ├── favoriteExp
    ├── defaultHunger
    └── defaultExp
```

## 🎯 总结

- ✅ **6个等级**，每级独立配置
- ✅ **完全可自定义**，无硬编码
- ✅ **JSON配置**，易于修改
- ✅ **实时生效**，重启游戏即可
- ✅ **灵活平衡**，适应不同玩法

祝您游戏愉快！🎮
