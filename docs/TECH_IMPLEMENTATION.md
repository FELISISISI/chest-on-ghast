# Chest on Ghast 技术实现文档（Fabric 1.21.9）

> 参考资料：  
> - Fabric 官方仓库（1.21.9 分支）— https://github.com/FabricMC/fabric/tree/1.21.9  
> - Chest on Ghast FEATURE 规格 — `FEATURES_GUIDE.md`

---

## 1. 范围与目标
- **目标**：在 Minecraft 1.21.9 + Fabric Loader 0.16.14 环境下，实现快乐恶魂（Happy Ghast）从成长、饱食度、战斗、附魔到 GUI 与配置的一整套玩法体系。
- **风格**：服务端主导逻辑、客户端渲染与交互，所有状态通过网络与数据持久化保持一致。
- **成功标准**：
  - 6 级成长体系、饱食度、火球战斗与效果云完整可用。
  - GUI、骑乘瞄准器、附魔管理功能稳定，与配置文件可热更新（重启后生效）。
  - 具备可扩展的日志（Winston）与诊断机制，覆盖主要逻辑路径。

---

## 2. 技术栈与依赖

| 组件 | 用途 | 官方文档定位 |
|------|------|--------------|
| `fabric-api`（1.21.9） | 基础工具集合 | https://github.com/FabricMC/fabric/tree/1.21.9 |
| `fabric-entity-api-v1` | 自定义实体注册、属性与目标选择 | `fabric-entity-events-v1`, `fabric-entity-attribute-api-v1` |
| `fabric-networking-api-v1` | C2S/S2C 自定义 Payload | `fabric-networking`, `payloading` 示例 |
| `fabric-screen-api-v1` + `fabric-key-binding-api-v1` | 客户端 GUI、按键绑定（瞄准镜） | `fabric-key-binding-api-v1/` |
| `fabric-data-attachment-api-v1` | 实体扩展数据（等级、饱食度） | `fabric-data-attachment-api-v1/` |
| `fabric-rendering-v1` | 自定义渲染/粒子 | `fabric-rendering-v1/` |
| `Winston`（Node 服务） | 统一日志与远程 Telemetry，通过 WebSocket/HTTP 收集 | 项目 `logging/` 子模块 |

> **注意**：在 `build.gradle` 中锁定 Fabric API 版本，防止与 1.21.9 运行时不兼容。

---

## 3. 架构概览

### 3.1 分层结构
- **Accessors & Data (`HappyGhastData`, `HappyGhastDataAccessor`)**：封装实体所有状态，负责 NBT 序列化。
- **Domain Services**：等级、饱食度、战斗、附魔、效果云等子系统，各自提供 `tick`、`sync`、`handlePayload` 等接口。
- **Networking (`network/*`, `NetworkHandler`)**：Fabric Payload（`ServerPlayNetworking`, `ClientPlayNetworking`）处理 GUI、同步与命名等事件。
- **Client UI (`gui/HappyGhastScreen`)**：ScreenHandler + Screen 组合显示状态、附魔槽与交互按钮。
- **Configuration (`config/GhastConfig`, `level/LevelConfig`)**：读取 JSON/JSR 规范的配置并缓存，支持热重载入口。
- **Logging & Telemetry**：使用 Winston 模块，暴露 `GhastLogger`（Java）→ HTTP/WebSocket → Node Winston pipeline。

### 3.2 数据流与同步
1. **实体 Tick**：`HappyGhastEntityMixin` 注入 `ticker`，驱动等级、饱食度、AI 验证。
2. **状态变更**：服务端修改 `HappyGhastData`，触发 `SyncGhastDataPayload` 推送客户端。
3. **客户端展示**：GUI/渲染读取最新缓存数据，骑乘瞄准镜按键通过 `GreetGhastPayload` 等回传。
4. **配置生效**：`GhastConfig` 启动时读取 `.minecraft/config/chest-on-ghast.json`，并为每级生成 `LevelConfig` 缓存。
5. **日志记录**：每个关键事件（升级、附魔变化、攻击、GUI 保存）调用 Winston 适配器。

---

## 4. 核心系统设计

### 4.1 等级系统
- **数据结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `level` | `int` | 1-6 |
| `exp` | `int` | 当前经验 |
| `expToNext` | `int` | 来自 `LevelConfig` |
| `maxHealth` / `maxHunger` | `float` | 配置驱动 |
| `fireballPower` | `int` | 控制爆炸强度 |
| `attackCooldown` | `int` (ticks) | 随等级降低 |

- **实现**：
  1. `LevelConfig` 按等级缓存表格数据（参考 FEATURES_GUIDE），通过 `GhastConfig#levels`.
  2. 喂食事件 → `HappyGhastData#addExperience`，判断是否超阈值→ `levelUp`.
  3. 升级 Hook：
     - 更新属性 (`EntityAttributeInstance` via Fabric Attribute API)。
     - 重置饱食度至 `maxHunger`。
     - 触发 `SyncGhastDataPayload`，并通过 Winston 记录 `info`.
  4. 3 级时触发解锁标志 `effectCloudUnlocked = true`。

### 4.2 饱食度系统
- **Tick**：`HappyGhastData#tickHunger` 根据 `hungerDecayMultiplier` 每秒减值，通过 `serverWorld.getTime()` 控制节奏。
- **交互**：喂食时同时 +10 饱食度，封顶 `maxHunger`。
- **惩罚**：`<=0` 后应用 `Weakness`、`Slowness` 并暂停自动攻击。
- **持久化**：通过 Data Attachment API / NBT `hunger` 字段。

### 4.3 战斗系统
- **AI 目标**：`TargetGoal` & `ProjectileAttackGoal` 自定义实现，搜索 16 格敌对生物。
- **火球**：
  - 使用 `LargeFireballEntity` 或自定义 `HappyGhastFireball`.
  - 威力基于 `LevelConfig.fireballPower`。
  - 冷却：存储 `lastAttackTick`，通过等级表控制。
- **DPS 表**：直接取自 FEATURES_GUIDE 以验证配置。
- **网络**：战斗逻辑仅运行在服务端，客户端靠数据包展示特效。

### 4.4 骑乘与瞄准镜
- **挽具检测**：实体 `DataTracker` 字段 `hasSaddle`，右键交互更新。
- **骑乘控制**：实现 `RideableInventory` 接口，使玩家操控移动矢量。
- **瞄准镜物品**：
  - `Item` 子类 + `use` 方法触发缩放 (`Scoped` shader)。
   - 客户端按键（KeyBinding API）记录按压时间 >= 10 ticks 后向服务端发送 `OpenGhastGuiPayload` 或自定义 `FireScopePayload`。
- **射击**：服务端校验骑乘+按键状态→执行火球发射。

### 4.5 效果云系统
- **解锁**：`level >= 3`。
- **生成流程**：
  1. 火球命中回调 → `spawnEffectCloud`.
  2. 创建 `AreaEffectCloudEntity`，读取 `cloudRadius`, `cloudDuration`。
  3. 应用效果列表：
     - 对怪物：`StatusEffects.INSTANT_DAMAGE` 等级随配置。
     - 对玩家：`REGENERATION`, `SPEED`.
     - 附魔（持久/冰冻/魅惑/引力/变形）通过 `EffectModifier` 接口叠加。
- **可配置**：全量字段映射 `GhastConfig`.
- **视觉**：自定义粒子（Fabric Rendering API），基于附魔类型选择不同 `ParticleEffect`。

### 4.6 附魔系统
- **数据结构**：3 个槽位 `List<EnchantmentSlot>`，记录 `type`, `level`, `context`.
- **实现策略**：
  - 注册自定义 `GhastEnchantment` 类，内部仅供快乐恶魂识别，不向原版附魔台投放。
  - GUI 操作通过 ScreenHandler Inventory 传输。
  - 每种附魔实现 `apply(AttackContext ctx)` / `apply(EffectCloudContext ctx)`。
- **穿透追踪（未实现）计划**：
  - 为火球附加 `targetChain` 队列，命中后寻找下一个目标。
  - 使用 `ProjectileUtil` + `EntityHitResult` 重算轨迹，同时注意性能（最大 5 次）。

### 4.7 GUI & 交互
- **打开方式**：`Shift + 右键` → 发送 `RequestGhastDataPayload`。
- **ScreenHandler**：
  - 槽位：名称输入、状态展示（只读）、3 个附魔槽 + 玩家物品栏。
  - `HappyGhastScreen` 负责渲染血量/饱食度/经验条。
- **数据同步**：打开 GUI 时服务器回发最新 `HappyGhastData`，关闭时客户端发送 `RenameGhastPayload`、`SyncEnchantPayload` 等。
- **无障碍**：支持鼠标提示、颜色区分。

### 4.8 配置系统
- **文件**：`.minecraft/config/chest-on-ghast.json`。
- **加载**：
  1. `GhastConfig#load(Path)` 在 Mod 初始化时执行。
  2. 构建 `ImmutableMap<Integer, LevelConfig>`.
  3. 若缺级别则回退默认值并输出 `warn`。
- **热更新**：命令 `/ghastconfig reload` 触发重新读取文件并推送在线实体（通过遍历世界实体并重建属性）。

### 4.9 网络协议

| Payload | 方向 | 说明 |
|---------|------|------|
| `RequestGhastDataPayload` | C→S | GUI 打开请求 |
| `SyncGhastDataPayload` | S→C | 推送实体数据 |
| `OpenGhastGuiPayload` | S→C | 强制打开 GUI（骑乘事件） |
| `RenameGhastPayload` | C→S | 保存自定义名称 |
| `GreetGhastPayload` | C→S | 交互/喂食、骑乘等高频事件 |

- **实现**：使用 Fabric 新 Payload API（参考官方 `fabric-networking-api-v1` 示例），`CustomPayload` + `PacketCodec`.
- **校验**：所有 C→S 包在服务端校验玩家是否与实体交互范围内，防止作弊。

### 4.10 数据持久化
- `HappyGhastDataAccessor` 将 `HappyGhastData` 注入实体 NBT。
- 关键字段：等级、经验、饱食度、附魔槽、解锁标志、命名。
- 在世界存档加载时自动恢复。

### 4.11 日志与 Telemetry（Winston）
- **Java 侧**：提供 `GhastLogger`（单例）封装 `WinstonClient`，在以下节点记录：
  - 等级变化（`info`）
  - 饱食度告警（`warn`）
  - 战斗/瞄准镜触发（`debug`）
  - 附魔/配置错误（`error`）
- **Node 侧**：`logging/winston.config.js` 统一输出到文件 + 远程（Elastic/Logstash）。
- **传输**：HTTP `POST /logs` 或 WebSocket；若失败则写回本地 `latest.log`.

### 4.12 测试计划
- **单元**：`HappyGhastDataTest` 覆盖等级、饱食度、附魔叠加逻辑。
- **集成**：使用 Fabric GameTest：
  - 战斗行为（自动锁定、冷却）。
  - 效果云效果矩阵。
  - GUI 打开与同步。
- **兼容性**：确保无 Fabric API 过时调用（对照 1.21.9 文档）；在专用服务器与客户端分别验证。

### 4.13 性能与风险
- **Tick 开销**：每只恶魂的 `tick` 控制在 <100µs；大部分计算缓存（如 `LevelConfig`）。
- **网络洪泛**：同步包采用脏标记节流（只在状态改变时发送）。
- **附魔组合**：注意带来大规模效果云（连射+持久），需限制最大实体/粒子数量。
- **穿透追踪**：待实现功能需重点关注 CPU 与网络增量。

---

## 5. 实现顺序建议
1. 配置/数据结构（Level & Hunger）。
2. 实体扩展 + 战斗基础。
3. HUD/GUI 与网络同步。
4. 附魔实现与特效。
5. 骑乘瞄准镜与 Scope 交互。
6. Winston 日志与遥测。
7. GameTest 套件 & 性能调优。

---

## 6. 运维与排障
- **日志管道**：优先查看 Winston 聚合面板；若离线则查 `.minecraft/logs/latest.log`.
- **配置问题**：执行 `/ghastconfig dump` 输出当前缓存。
- **网络同步**：启用 `fabric-networking-debug`（官方示例）观察 Payload。
- **崩溃分析**：保留 `错误报告-*` 目录中的崩溃堆栈，结合 Fabric 官方 issue tracker。

---

## 7. 附件
- Level/饱食度/效果云表格：详见 `FEATURES_GUIDE.md`。
- Fabric 1.21.9 API 文档：`fabric-api/*/README.md`（官方仓库目录结构）。
- 配置示例：`LEVELING_SYSTEM_README.md`、`GhastConfig` 默认值。

> 本文档旨在作为开发与维护指南，后续若拓展新附魔或跨模组联动，请在相应章节追加设计与依赖说明。

