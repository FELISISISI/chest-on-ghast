# 快乐恶魂模组技术实现文档

> **Chest on Ghast Mod - Technical Implementation Document**  
> Minecraft 版本：1.21.9 | Fabric Loader 0.16.14+ | Fabric API 0.117.3+  
> Java 版本：21

---

## 📋 目录

1. [项目架构概览](#项目架构概览)
2. [核心模块设计](#核心模块设计)
3. [已实现功能技术方案](#已实现功能技术方案)
4. [待实现功能技术方案](#待实现功能技术方案)
5. [数据流与网络通信](#数据流与网络通信)
6. [配置系统](#配置系统)
7. [测试与调试策略](#测试与调试策略)
8. [性能优化建议](#性能优化建议)
9. [扩展性设计](#扩展性设计)

---

## 项目架构概览

### 模组基本信息

```
模组ID: chest-on-ghast
主类: me.noramibu.Chestonghast (服务端)
客户端类: me.noramibu.ChestonghastClient (客户端)
```

### 技术栈

- **Fabric Loader**: 0.16.14+
- **Fabric API**: 完整API (networking, events, registry)
- **Mixin**: ASM字节码注入技术
- **Gson**: JSON配置文件序列化/反序列化
- **SLF4J**: 日志记录框架

### 模块划分

```
src/main/java/me/noramibu/
├── Chestonghast.java              # 模组主入口（服务端）
├── ChestonghastClient.java        # 客户端入口
├── NetworkHandler.java            # 网络包注册与处理
├── accessor/                      # 数据访问器接口
│   └── HappyGhastDataAccessor.java
├── config/                        # 配置系统
│   └── GhastConfig.java
├── data/                          # 数据模型
│   └── HappyGhastData.java
├── gui/                           # GUI界面
│   └── HappyGhastScreen.java
├── level/                         # 等级系统
│   └── LevelConfig.java
├── mixin/                         # Mixin注入
│   └── HappyGhastEntityMixin.java
└── network/                       # 网络包定义
    ├── GreetGhastPayload.java
    ├── OpenGhastGuiPayload.java
    ├── RenameGhastPayload.java
    ├── RequestGhastDataPayload.java
    └── SyncGhastDataPayload.java
```

### 设计模式

- **Mixin模式**: 使用Mixin注入原版HappyGhastEntity，避免直接修改源码
- **Accessor模式**: 通过接口安全地访问和存储实体数据
- **单例模式**: 配置管理器使用单例模式
- **观察者模式**: GUI定期向服务器请求数据更新
- **策略模式**: 不同附魔效果使用不同策略实现

---

## 核心模块设计

### 1. 实体数据管理 (HappyGhastData)

#### 数据结构

```java
public class HappyGhastData {
    private int level;                     // 等级 (1-6)
    private int experience;                // 经验值
    private float hunger;                  // 当前饱食度
    private long lastHungerDecayTime;      // 上次饱食度更新时间
    private List<String> favoriteFoods;    // 最喜欢的食物 (3个)
    private String customName;             // 自定义名字
    private Map<String, Integer> enchantments; // 附魔系统 (待实现)
}
```

#### NBT持久化

**序列化策略**:
- 使用`NbtCompound`存储基础数据类型
- 使用`NbtList`存储列表数据
- 使用Minecraft的`Optional`机制安全读取NBT

**关键代码**:
```java
public void writeToNbt(NbtCompound nbt) {
    nbt.putInt("Level", level);
    nbt.putInt("Experience", experience);
    nbt.putFloat("Hunger", hunger);
    nbt.putLong("LastHungerDecayTime", lastHungerDecayTime);
    
    // 保存最喜欢的食物列表
    NbtList foodList = new NbtList();
    for (String food : favoriteFoods) {
        foodList.add(NbtString.of(food));
    }
    nbt.put("FavoriteFoods", foodList);
}
```

### 2. Mixin注入系统 (HappyGhastEntityMixin)

#### Mixin目标
```java
@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin extends MobEntity 
    implements HappyGhastDataAccessor
```

#### 注入点

1. **构造函数注入** (`<init>`)
   - 初始化HappyGhastData
   - 注册AI Goals

2. **Tick注入** (`tick`)
   - 更新饱食度
   - 同步血量上限
   - 服务端逻辑

3. **交互注入** (`interactMob`)
   - 处理喂食
   - 打开GUI
   - 装备挽具

#### AI Goal系统

**FollowPlayerWithFoodGoal**: 自定义AI Goal
```java
private static class FollowPlayerWithFoodGoal extends Goal {
    // 快乐恶魂会跟随手持食物的玩家
    // 检测范围: 6格
    // 最小距离: 3格
    // 移动速度: 1.0
}
```

### 3. 网络通信架构

#### 网络包类型

| 包名 | 方向 | 用途 | Codec |
|------|------|------|-------|
| GreetGhastPayload | C2S | H键问候 | PacketCodec |
| OpenGhastGuiPayload | C2S | 打开GUI请求 | PacketCodec |
| RequestGhastDataPayload | C2S | 请求数据更新 | PacketCodec |
| RenameGhastPayload | C2S | 改名请求 | PacketCodec |
| SyncGhastDataPayload | S2C | 同步数据到客户端 | PacketCodec |

#### 网络包注册 (Fabric Networking API v1)

**服务端注册**:
```java
public static void registerServerReceivers() {
    // 注册C2S包
    PayloadTypeRegistry.playC2S().register(
        GreetGhastPayload.ID, 
        GreetGhastPayload.CODEC
    );
    
    // 注册S2C包
    PayloadTypeRegistry.playS2C().register(
        SyncGhastDataPayload.ID,
        SyncGhastDataPayload.CODEC
    );
    
    // 注册接收器
    ServerPlayNetworking.registerGlobalReceiver(
        GreetGhastPayload.ID,
        (payload, context) -> {
            context.server().execute(() -> {
                // 处理逻辑
            });
        }
    );
}
```

**客户端注册**:
```java
ClientPlayNetworking.registerGlobalReceiver(
    SyncGhastDataPayload.ID,
    (payload, context) -> {
        context.client().execute(() -> {
            // 打开或更新GUI
        });
    }
);
```

### 4. 配置系统 (GhastConfig)

#### 配置文件结构

```json
{
  "levels": {
    "1": {
      "maxHealth": 20.0,
      "maxHunger": 100.0,
      "expToNextLevel": 100,
      "hungerDecayMultiplier": 1.0
    },
    ...
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

#### 配置加载流程

```
启动 → 检查配置文件
      ↓
   存在? → 加载并验证
      ↓
  不存在/无效? → 创建默认配置
      ↓
   返回配置实例
```

---

## 已实现功能技术方案

### ✅ 等级系统 (6级)

#### 实现架构

**LevelConfig类**: 存储每个等级的配置
```java
public static class LevelData {
    private final int level;
    private final float maxHealth;
    private final float maxHunger;
    private final int expToNextLevel;
    private final float hungerDecayRate;
}
```

**升级逻辑**:
```java
public boolean addExperience(int amount) {
    this.experience += amount;
    boolean leveledUp = false;
    
    // 连续升级检测
    while (LevelConfig.canLevelUp(level, experience)) {
        levelUp();
        leveledUp = true;
    }
    
    return leveledUp;
}
```

**血量上限同步**:
```java
private void updateMaxHealth(HappyGhastEntity ghast) {
    float maxHealth = ghastData.getMaxHealth();
    var maxHealthAttribute = ghast.getAttributeInstance(
        EntityAttributes.MAX_HEALTH
    );
    
    if (maxHealthAttribute != null) {
        maxHealthAttribute.setBaseValue(maxHealth);
        
        // 确保当前血量不超过上限
        if (ghast.getHealth() > maxHealth) {
            ghast.setHealth(maxHealth);
        }
    }
}
```

### ✅ 饱食度系统

#### 消耗机制

**每秒更新策略**:
```java
public void updateHunger() {
    long currentTime = System.currentTimeMillis();
    long timeDiff = currentTime - lastHungerDecayTime;
    
    // 每1000ms更新一次
    if (timeDiff >= 1000) {
        float decayAmount = levelData.getHungerDecayRate();
        float totalDecay = decayAmount * (timeDiff / 1000.0f);
        this.hunger = Math.max(0, this.hunger - totalDecay);
        this.lastHungerDecayTime = currentTime;
    }
}
```

**消耗速率计算**:
```
基础速率 = 最大饱食度 / 1200秒（一MC昼夜）
实际速率 = 基础速率 × 等级消耗倍率
```

#### 喂食系统

**食物经验值映射**:
```java
public static float[] getFoodValues(String foodItem, boolean isFavorite) {
    if (isFavorite) {
        return new float[]{80.0f, 50}; // [饱食度, 经验值]
    }
    
    return switch (foodItem) {
        case "minecraft:apple" -> new float[]{10.0f, 5};
        case "minecraft:golden_apple" -> new float[]{30.0f, 30};
        case "minecraft:enchanted_golden_apple" -> new float[]{40.0f, 50};
        default -> new float[]{12.0f, 5};
    };
}
```

**最喜欢的食物系统**:
- 每只快乐恶魂随机分配3个最喜欢的食物
- 喂食最喜欢的食物时：
  - 恢复80饱食度（vs 普通10-40）
  - 获得50经验（vs 普通5-30）
  - 生成7个爱心粒子效果
  - 显示特殊消息

### ✅ 战斗系统

#### 自动攻击AI

**实现方式**: 
- 暂未实现完整的AI攻击系统
- 需要添加`AttackGoal`或`ShootFireballGoal`

**建议实现**:
```java
@Unique
private static class ShootFireballGoal extends Goal {
    private final HappyGhastEntity ghast;
    private LivingEntity target;
    private int cooldown;
    
    @Override
    public boolean canStart() {
        // 在16格内寻找敌对生物
        target = ghast.getEntityWorld().getClosestEntity(
            HostileEntity.class,
            TargetPredicate.createAttackable(),
            ghast,
            ghast.getX(), ghast.getY(), ghast.getZ(),
            ghast.getBoundingBox().expand(16.0)
        );
        
        return target != null && cooldown <= 0;
    }
    
    @Override
    public void tick() {
        if (target != null && cooldown <= 0) {
            // 发射火球
            shootFireball();
            
            // 根据等级设置冷却时间
            cooldown = getCooldownByLevel();
        }
        cooldown--;
    }
}
```

### ✅ 骑乘系统

#### 挽具装备

**检测逻辑** (在`interactMob`中):
```java
if (itemStack.isOf(Items.SADDLE)) {
    // 装备挽具到身体槽位
    ghast.equipStack(EquipmentSlot.BODY, new ItemStack(Items.SADDLE));
    
    if (!player.getAbilities().creativeMode) {
        itemStack.decrement(1);
    }
    
    return ActionResult.SUCCESS;
}
```

**骑乘逻辑**:
```java
if (!itemStack.isEmpty() && itemStack.isOf(Items.SADDLE)) {
    // 如果已经装备挽具且玩家点击，开始骑乘
    if (!ghast.getEquippedStack(EquipmentSlot.BODY).isEmpty()) {
        player.startRiding(ghast);
        return ActionResult.SUCCESS;
    }
}
```

### ✅ GUI系统

#### HappyGhastScreen设计

**布局结构**:
```
┌─────────────────────────────────────┐
│     [名字输入框]                      │
│                                     │
│          等级 X                      │
│                                     │
│  ┌─血量─┐  ┌─饱食度─┐  ┌─经验值─┐    │
│  │ HP   │  │ HUNGER│  │  EXP  │    │
│  │█████ │  │████   │  │███    │    │
│  │100/20│  │80/100 │  │50/100 │    │
│  └──────┘  └───────┘  └───────┘    │
│                                     │
│     按ESC关闭                        │
└─────────────────────────────────────┘
```

**实时数据同步**:
- 每10 ticks (0.5秒) 向服务器请求数据
- 使用`RequestGhastDataPayload`请求
- 服务器返回`SyncGhastDataPayload`

**名字输入框**:
```java
private TextFieldWidget nameField;

// 失去焦点时发送改名请求
if (wasFocused && !currentlyFocused) {
    String newName = nameField.getText();
    if (!newName.equals(customName)) {
        ClientPlayNetworking.send(
            new RenameGhastPayload(entityId, newName)
        );
    }
}
```

---

## 待实现功能技术方案

### ❌ 效果云系统 (3级解锁)

#### 核心实现思路

**1. 火球击中检测**

在火球爆炸时触发效果云生成：

```java
// 在HappyGhastEntityMixin中添加
@Mixin(FireballEntity.class)
public class FireballEntityMixin {
    @Inject(method = "onCollision", at = @At("HEAD"))
    private void onFireballHit(HitResult hitResult, CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        
        // 检查火球的所有者是否为快乐恶魂
        if (fireball.getOwner() instanceof HappyGhastEntity ghast) {
            HappyGhastData data = getGhastData(ghast);
            
            // 检查是否达到3级
            if (data.getLevel() >= 3) {
                spawnEffectCloud(ghast, hitResult.getPos(), data);
            }
        }
    }
}
```

**2. 效果云生成**

```java
private void spawnEffectCloud(HappyGhastEntity ghast, Vec3d pos, HappyGhastData data) {
    if (ghast.getWorld() instanceof ServerWorld serverWorld) {
        // 创建区域效果云
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
            serverWorld, 
            pos.x, pos.y, pos.z
        );
        
        // 根据等级配置效果云
        float radius = getCloudRadius(data.getLevel());
        int duration = getCloudDuration(data.getLevel());
        
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setParticleType(ParticleTypes.HAPPY_VILLAGER);
        cloud.setRadiusGrowth(-0.005f); // 缓慢缩小
        
        // 为怪物添加伤害效果
        StatusEffectInstance damageEffect = new StatusEffectInstance(
            StatusEffects.INSTANT_DAMAGE,
            1, // 立即生效
            getDamageAmplifier(data.getLevel())
        );
        cloud.addEffect(damageEffect);
        
        // 为玩家添加治疗效果
        StatusEffectInstance healEffect = new StatusEffectInstance(
            StatusEffects.REGENERATION,
            duration,
            getRegenAmplifier(data.getLevel())
        );
        // 注意：需要自定义逻辑区分玩家和怪物
        
        serverWorld.spawnEntity(cloud);
    }
}
```

**3. 区分玩家和怪物的效果**

由于原版`AreaEffectCloudEntity`不支持区分目标，需要使用自定义逻辑：

```java
@Mixin(AreaEffectCloudEntity.class)
public class AreaEffectCloudEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onCloudTick(CallbackInfo ci) {
        AreaEffectCloudEntity cloud = (AreaEffectCloudEntity) (Object) this;
        
        // 检查是否为快乐恶魂的效果云（通过NBT标记）
        if (isHappyGhastCloud(cloud)) {
            // 手动应用效果
            applyCustomEffects(cloud);
            ci.cancel(); // 取消原版逻辑
        }
    }
    
    private void applyCustomEffects(AreaEffectCloudEntity cloud) {
        List<Entity> entities = cloud.getWorld().getOtherEntities(
            cloud,
            cloud.getBoundingBox(),
            entity -> entity instanceof LivingEntity
        );
        
        for (Entity entity : entities) {
            if (entity instanceof HostileEntity) {
                // 对怪物造成瞬间伤害
                applyDamageEffect((LivingEntity) entity);
            } else if (entity instanceof PlayerEntity) {
                // 对玩家施加治疗
                applyHealEffect((LivingEntity) entity);
            }
        }
    }
}
```

**4. 配置参数**

```java
private float getCloudRadius(int level) {
    return switch (level) {
        case 3 -> 3.0f;
        case 4 -> 3.5f;
        case 5 -> 4.0f;
        case 6 -> 5.0f;
        default -> 3.0f;
    };
}

private int getCloudDuration(int level) {
    return switch (level) {
        case 3 -> 100; // 5秒
        case 4 -> 120; // 6秒
        case 5 -> 140; // 7秒
        case 6 -> 160; // 8秒
        default -> 100;
    };
}
```

### ❌ 瞄准镜系统

#### 自定义物品注册

**1. 注册瞄准镜物品**

```java
public class ModItems {
    public static final Item HAPPY_GHAST_SCOPE = Registry.register(
        Registries.ITEM,
        Identifier.of("chest-on-ghast", "happy_ghast_scope"),
        new HappyGhastScopeItem(new Item.Settings().maxCount(1))
    );
    
    public static void registerItems() {
        Chestonghast.LOGGER.info("注册快乐恶魂瞄准镜物品");
    }
}
```

**2. 瞄准镜物品类**

```java
public class HappyGhastScopeItem extends Item {
    public HappyGhastScopeItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // 检查玩家是否在骑乘快乐恶魂
        if (player.getVehicle() instanceof HappyGhastEntity ghast) {
            // 开始使用（按住右键）
            player.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }
        
        return TypedActionResult.fail(stack);
    }
    
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // 每tick检查是否达到0.5秒（10 ticks）
        int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
        
        if (usedTicks >= 10) {
            // 应用望远镜效果（视野放大）
            if (user instanceof PlayerEntity player) {
                applySpyglassEffect(player);
            }
        }
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        // 松开右键时发射火球
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (player.getVehicle() instanceof HappyGhastEntity ghast) {
                shootFireball(ghast, player);
                player.sendMessage(
                    Text.translatable("message.chest-on-ghast.fireball_shot"),
                    true
                );
            }
        }
        
        return stack;
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000; // 允许长时间持续使用
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPYGLASS; // 使用望远镜动画
    }
    
    private void applySpyglassEffect(PlayerEntity player) {
        // 客户端效果：视野放大
        // 实际由UseAction.SPYGLASS自动处理
    }
    
    private void shootFireball(HappyGhastEntity ghast, ServerPlayerEntity player) {
        // 获取玩家视线方向
        Vec3d lookVec = player.getRotationVec(1.0f);
        
        // 创建火球
        FireballEntity fireball = new FireballEntity(
            ghast.getWorld(),
            ghast,
            lookVec,
            getPowerByLevel(ghast)
        );
        
        // 设置火球位置（从快乐恶魂前方发射）
        fireball.setPosition(
            ghast.getX() + lookVec.x * 2,
            ghast.getY() + 1.5,
            ghast.getZ() + lookVec.z * 2
        );
        
        // 生成火球
        ghast.getWorld().spawnEntity(fireball);
        
        // 播放音效
        ghast.getWorld().playSound(
            null,
            ghast.getBlockPos(),
            SoundEvents.ENTITY_GHAST_SHOOT,
            SoundCategory.HOSTILE,
            1.0f,
            1.2f // 音调提高20%
        );
    }
}
```

**3. 合成配方**

创建文件: `src/main/resources/data/chest-on-ghast/recipe/happy_ghast_scope.json`

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "GGG",
    "GEG",
    " S "
  ],
  "key": {
    "G": {
      "item": "minecraft:glass"
    },
    "E": {
      "item": "minecraft:ender_eye"
    },
    "S": {
      "item": "minecraft:stick"
    }
  },
  "result": {
    "id": "chest-on-ghast:happy_ghast_scope",
    "count": 1
  }
}
```

### ❌ 附魔系统

#### 架构设计

**1. 附魔数据结构**

```java
public class EnchantmentData {
    private String enchantmentId;    // 附魔ID
    private int level;                // 附魔等级 (I/II/III)
    
    public enum EnchantmentType {
        MULTISHOT,        // 连射
        DURATION,         // 持久
        FREEZING,         // 冰冻
        CHARM,            // 魅惑
        GRAVITY,          // 引力奇点
        POLYMORPH,        // 变形
        PIERCING_TRACKER  // 穿透追踪（未实现）
    }
}
```

**2. 附魔书物品**

```java
public class HappyGhastEnchantmentBook extends Item {
    private final EnchantmentType type;
    private final int level;
    
    public HappyGhastEnchantmentBook(EnchantmentType type, int level) {
        super(new Settings().maxCount(1).rarity(Rarity.RARE));
        this.type = type;
        this.level = level;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, 
                              List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(
            "enchantment.chest-on-ghast." + this.type.name().toLowerCase(),
            getRomanNumeral(level)
        ).formatted(Formatting.GRAY));
        
        tooltip.add(Text.translatable(
            "enchantment.chest-on-ghast." + this.type.name().toLowerCase() + ".desc"
        ).formatted(Formatting.DARK_GRAY));
    }
}
```

**3. 附魔槽位GUI**

扩展`HappyGhastScreen`：

```java
public class EnchantmentEditScreen extends HandledScreen<EnchantmentEditScreenHandler> {
    private static final int ENCHANTMENT_SLOTS = 3;
    
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 绘制3个附魔槽位
        for (int i = 0; i < ENCHANTMENT_SLOTS; i++) {
            int x = centerX - 60 + i * 40;
            int y = topY + 20;
            
            drawEnchantmentSlot(context, x, y, i);
        }
    }
    
    private void drawEnchantmentSlot(DrawContext context, int x, int y, int slotIndex) {
        // 绘制槽位背景
        context.drawTexture(
            TEXTURE,
            x, y, 18, 18,
            0, 0, 18, 18,
            256, 256
        );
        
        // 如果有附魔，显示附魔书图标
        EnchantmentData enchant = getEnchantment(slotIndex);
        if (enchant != null) {
            context.drawItem(
                new ItemStack(Items.ENCHANTED_BOOK),
                x + 1, y + 1
            );
            
            // 显示等级
            context.drawText(
                textRenderer,
                getRomanNumeral(enchant.level),
                x + 10, y + 10,
                0xFFFFFFFF,
                true
            );
        } else {
            // 空槽位显示"+"号
            context.drawText(
                textRenderer,
                "+",
                x + 6, y + 5,
                0xFF888888,
                false
            );
        }
    }
}
```

**4. 附魔效果应用**

**连射附魔 (Multishot)**:
```java
private void shootMultishotFireballs(HappyGhastEntity ghast, LivingEntity target, int count) {
    // 计算扇形角度
    float angleSpread = switch (count) {
        case 3 -> 15.0f;
        case 5 -> 22.5f;
        case 7 -> 30.0f;
        default -> 0.0f;
    };
    
    float startAngle = -angleSpread / 2;
    float angleStep = angleSpread / (count - 1);
    
    for (int i = 0; i < count; i++) {
        float angle = startAngle + angleStep * i;
        Vec3d direction = rotateVector(getDirectionToTarget(ghast, target), angle);
        
        FireballEntity fireball = new FireballEntity(
            ghast.getWorld(),
            ghast,
            direction,
            getPower(ghast)
        );
        
        ghast.getWorld().spawnEntity(fireball);
    }
    
    // 播放特殊音效（音调提高）
    ghast.getWorld().playSound(
        null, ghast.getBlockPos(),
        SoundEvents.ENTITY_GHAST_SHOOT,
        SoundCategory.HOSTILE,
        1.0f, 1.2f
    );
}
```

**持久附魔 (Duration)**:
```java
private int getEffectCloudDuration(HappyGhastEntity ghast) {
    int baseDuration = getBaseDuration(ghast.getLevel());
    
    EnchantmentData durationEnchant = getEnchantment(EnchantmentType.DURATION);
    if (durationEnchant != null) {
        float multiplier = switch (durationEnchant.level) {
            case 1 -> 1.5f;
            case 2 -> 2.0f;
            case 3 -> 3.0f;
            default -> 1.0f;
        };
        
        return (int) (baseDuration * multiplier);
    }
    
    return baseDuration;
}
```

**冰冻附魔 (Freezing)**:
```java
@Mixin(AreaEffectCloudEntity.class)
public class FreezingCloudMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void applyFreezingEffect(CallbackInfo ci) {
        AreaEffectCloudEntity cloud = (AreaEffectCloudEntity) (Object) this;
        
        if (hasFreezingEnchantment(cloud)) {
            List<LivingEntity> entities = getEntitiesInCloud(cloud);
            int enchantLevel = getFreezingLevel(cloud);
            
            for (LivingEntity entity : entities) {
                if (entity instanceof HostileEntity) {
                    // 应用缓慢效果
                    int slownessLevel = switch (enchantLevel) {
                        case 1 -> 5;  // 缓慢 V
                        case 2 -> 7;  // 缓慢 VII
                        case 3 -> 10; // 缓慢 X
                        default -> 5;
                    };
                    
                    int duration = switch (enchantLevel) {
                        case 1 -> 60;  // 3秒
                        case 2 -> 100; // 5秒
                        case 3 -> 160; // 8秒
                        default -> 60;
                    };
                    
                    entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        duration,
                        slownessLevel - 1
                    ));
                    
                    entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.MINING_FATIGUE,
                        duration,
                        slownessLevel - 1
                    ));
                    
                    // 生成雪花粒子
                    if (cloud.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(
                            ParticleTypes.SNOWFLAKE,
                            entity.getX(), entity.getY() + 1, entity.getZ(),
                            5, 0.5, 0.5, 0.5, 0.0
                        );
                    }
                }
            }
        }
    }
}
```

**魅惑附魔 (Charm)**:
```java
@Mixin(AreaEffectCloudEntity.class)
public class CharmCloudMixin {
    private int charmTick = 0;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void applyCharmEffect(CallbackInfo ci) {
        AreaEffectCloudEntity cloud = (AreaEffectCloudEntity) (Object) this;
        
        if (hasCharmEnchantment(cloud)) {
            charmTick++;
            
            // 每10 ticks (0.5秒) 触发一次
            if (charmTick >= 10) {
                charmTick = 0;
                
                List<HostileEntity> hostiles = getHostilesInCloud(cloud);
                int enchantLevel = getCharmLevel(cloud);
                
                float damage = switch (enchantLevel) {
                    case 1 -> 2.0f;
                    case 2 -> 4.0f;
                    case 3 -> 6.0f;
                    default -> 2.0f;
                };
                
                // 让怪物互相攻击
                for (HostileEntity attacker : hostiles) {
                    if (hostiles.size() > 1) {
                        // 随机选择另一个怪物作为目标
                        HostileEntity target = getRandomOther(hostiles, attacker);
                        
                        // 设置攻击目标
                        if (attacker instanceof MobEntity mob) {
                            mob.setTarget(target);
                        }
                        
                        // 直接造成伤害
                        target.damage(
                            cloud.getWorld().getDamageSources().mobAttack(attacker),
                            damage
                        );
                        
                        // 生成愤怒粒子
                        if (cloud.getWorld() instanceof ServerWorld serverWorld) {
                            serverWorld.spawnParticles(
                                ParticleTypes.ANGRY_VILLAGER,
                                target.getX(), target.getY() + 1, target.getZ(),
                                3, 0.3, 0.3, 0.3, 0.0
                            );
                        }
                    }
                }
            }
        }
    }
}
```

**引力奇点附魔 (Gravity Singularity)**:
```java
@Mixin(FireballEntity.class)
public class GravityFireballMixin {
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onGravityFireballHit(HitResult hitResult, CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        
        if (hasGravityEnchantment(fireball)) {
            // 取消爆炸
            ci.cancel();
            
            // 生成引力奇点效果云
            createGravitySingularity(fireball, hitResult.getPos());
        }
    }
    
    private void createGravitySingularity(FireballEntity fireball, Vec3d pos) {
        if (!(fireball.getWorld() instanceof ServerWorld serverWorld)) return;
        
        int enchantLevel = getGravityLevel(fireball);
        float range = switch (enchantLevel) {
            case 1 -> 5.0f;
            case 2 -> 8.0f;
            case 3 -> 12.0f;
            default -> 5.0f;
        };
        
        float strength = switch (enchantLevel) {
            case 1 -> 0.15f;
            case 2 -> 0.25f;
            case 3 -> 0.40f;
            default -> 0.15f;
        };
        
        // 创建效果云作为奇点中心
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
            serverWorld, pos.x, pos.y, pos.z
        );
        cloud.setRadius(range);
        cloud.setDuration(getDuration(fireball));
        cloud.setParticleType(ParticleTypes.PORTAL);
        
        // 标记为引力云
        cloud.getDataTracker().set(IS_GRAVITY_CLOUD, true);
        
        serverWorld.spawnEntity(cloud);
        
        // 开始引力效果任务
        startGravityPulling(serverWorld, pos, range, strength, getDuration(fireball));
    }
    
    private void startGravityPulling(ServerWorld world, Vec3d center, 
                                     float range, float strength, int duration) {
        // 使用服务器调度器每2 ticks拉取一次实体
        int[] ticksRemaining = {duration};
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ticksRemaining[0] <= 0) return;
            
            ticksRemaining[0] -= 2;
            
            // 获取范围内的实体
            List<Entity> entities = world.getOtherEntities(
                null,
                Box.of(center, range * 2, range * 2, range * 2),
                entity -> entity instanceof HostileEntity || entity instanceof ItemEntity
            );
            
            // 限制数量以优化性能
            int monsterCount = 0;
            int itemCount = 0;
            
            for (Entity entity : entities) {
                if (entity instanceof HostileEntity && monsterCount < 30) {
                    pullEntity(entity, center, strength);
                    monsterCount++;
                } else if (entity instanceof ItemEntity && itemCount < 50) {
                    pullEntity(entity, center, strength * 1.5f);
                    itemCount++;
                }
            }
            
            // 生成粒子效果
            spawnGravityParticles(world, center, range);
        });
    }
    
    private void pullEntity(Entity entity, Vec3d center, float strength) {
        Vec3d toCenter = center.subtract(entity.getPos());
        double distance = toCenter.length();
        
        if (distance < 0.5) return; // 太近不再拉
        
        // 引力强度随距离衰减（平方反比）
        double pullStrength = strength / (distance * distance);
        
        Vec3d pullVelocity = toCenter.normalize().multiply(pullStrength);
        entity.setVelocity(entity.getVelocity().add(pullVelocity));
        entity.velocityModified = true;
    }
    
    private void spawnGravityParticles(ServerWorld world, Vec3d center, float range) {
        // 旋转粒子环
        int particleCount = 20;
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double x = center.x + Math.cos(angle) * range * 0.8;
            double z = center.z + Math.sin(angle) * range * 0.8;
            
            world.spawnParticles(
                ParticleTypes.PORTAL,
                x, center.y, z,
                1, 0, 0, 0, 0.1
            );
        }
        
        // 中心黑色烟雾
        world.spawnParticles(
            ParticleTypes.LARGE_SMOKE,
            center.x, center.y, center.z,
            5, 0.2, 0.2, 0.2, 0.0
        );
    }
}
```

**变形附魔 (Polymorph)**:
```java
@Mixin(AreaEffectCloudEntity.class)
public class PolymorphCloudMixin {
    private final Set<UUID> transformedEntities = new HashSet<>();
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void applyPolymorphEffect(CallbackInfo ci) {
        AreaEffectCloudEntity cloud = (AreaEffectCloudEntity) (Object) this;
        
        if (hasPolymorphEnchantment(cloud)) {
            List<HostileEntity> hostiles = getHostilesInCloud(cloud);
            int enchantLevel = getPolymorphLevel(cloud);
            
            float chance = switch (enchantLevel) {
                case 1 -> 0.33f;
                case 2 -> 0.66f;
                case 3 -> 1.0f;
                default -> 0.33f;
            };
            
            Random random = new Random();
            
            for (HostileEntity hostile : hostiles) {
                // 避免重复变形
                if (transformedEntities.contains(hostile.getUuid())) {
                    continue;
                }
                
                // 概率检测
                if (random.nextFloat() <= chance) {
                    transformToPassiveMob(hostile, cloud.getWorld());
                    transformedEntities.add(hostile.getUuid());
                }
            }
        }
    }
    
    private void transformToPassiveMob(HostileEntity hostile, World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        // 保存位置和名字
        Vec3d pos = hostile.getPos();
        float yaw = hostile.getYaw();
        float pitch = hostile.getPitch();
        Text customName = hostile.getCustomName();
        
        // 随机选择被动生物
        EntityType<?>[] passiveMobs = {
            EntityType.CHICKEN,
            EntityType.RABBIT,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.COW
        };
        
        EntityType<?> targetType = passiveMobs[
            new Random().nextInt(passiveMobs.length)
        ];
        
        // 创建新实体
        Entity newEntity = targetType.create(serverWorld);
        if (newEntity != null) {
            newEntity.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
            
            // 继承名字
            if (customName != null) {
                newEntity.setCustomName(customName);
                newEntity.setCustomNameVisible(true);
            }
            
            // 生成新实体
            serverWorld.spawnEntity(newEntity);
            
            // 移除原怪物（不掉落物品）
            hostile.discard();
            
            // 华丽的粒子效果（4层）
            spawnPolymorphParticles(serverWorld, pos);
            
            // 音效
            serverWorld.playSound(
                null, pos.x, pos.y, pos.z,
                SoundEvents.ITEM_TOTEM_USE,
                SoundCategory.NEUTRAL,
                1.0f, 1.5f
            );
        }
    }
    
    private void spawnPolymorphParticles(ServerWorld world, Vec3d pos) {
        // 1. 不死图腾粒子（金色）
        world.spawnParticles(
            ParticleTypes.TOTEM_OF_UNDYING,
            pos.x, pos.y + 1, pos.z,
            30, 0.5, 0.5, 0.5, 0.1
        );
        
        // 2. 爆炸粒子（白色闪光）
        world.spawnParticles(
            ParticleTypes.EXPLOSION,
            pos.x, pos.y + 1, pos.z,
            5, 0.3, 0.3, 0.3, 0.0
        );
        
        // 3. 快乐村民粒子（绿色爱心）
        world.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            pos.x, pos.y + 1, pos.z,
            20, 0.5, 0.5, 0.5, 0.0
        );
        
        // 4. 传送门粒子（紫色烟雾）
        world.spawnParticles(
            ParticleTypes.PORTAL,
            pos.x, pos.y + 1, pos.z,
            50, 0.5, 0.5, 0.5, 0.5
        );
    }
}
```

### ❌ 穿透追踪附魔 (Piercing Tracker)

#### 实现思路

这个附魔需要修改火球的碰撞逻辑，使其能够：
1. 穿透第一个目标而不消失
2. 追踪下一个敌对生物
3. 连续击中多个目标

**核心实现**:

```java
@Mixin(FireballEntity.class)
public class PiercingFireballMixin {
    @Unique
    private int piercingTargetsRemaining = 0;
    @Unique
    private final Set<UUID> hitEntities = new HashSet<>();
    @Unique
    private LivingEntity nextTarget = null;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onFireballCreate(CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        
        if (hasPiercingEnchantment(fireball)) {
            int enchantLevel = getPiercingLevel(fireball);
            this.piercingTargetsRemaining = switch (enchantLevel) {
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 5;
                default -> 1;
            };
        }
    }
    
    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onPiercingHit(EntityHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        
        if (hasPiercingEnchantment(fireball) && piercingTargetsRemaining > 0) {
            Entity hitEntity = hitResult.getEntity();
            
            // 如果已经击中过这个实体，跳过
            if (hitEntities.contains(hitEntity.getUuid())) {
                cir.setReturnValue(false);
                return;
            }
            
            // 记录击中的实体
            hitEntities.add(hitEntity.getUuid());
            piercingTargetsRemaining--;
            
            // 造成伤害但不让火球消失
            hitEntity.damage(
                fireball.getWorld().getDamageSources().fireball(fireball, fireball.getOwner()),
                getPower(fireball)
            );
            
            // 生成爆炸粒子
            spawnHitParticles(fireball.getWorld(), hitResult.getPos());
            
            // 如果还能继续穿透，寻找下一个目标
            if (piercingTargetsRemaining > 0) {
                nextTarget = findNextTarget(fireball, hitEntity);
                
                if (nextTarget != null) {
                    // 调整火球方向追踪新目标
                    redirectFireball(fireball, nextTarget);
                }
            } else {
                // 达到最大穿透次数，让火球爆炸
                fireball.discard();
            }
            
            // 取消原版碰撞逻辑
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTrackingTick(CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;
        
        // 如果有追踪目标，持续调整方向
        if (nextTarget != null && nextTarget.isAlive()) {
            redirectFireball(fireball, nextTarget);
        } else if (piercingTargetsRemaining > 0) {
            // 目标死亡，寻找新目标
            nextTarget = findNextTarget(fireball, null);
        }
    }
    
    private LivingEntity findNextTarget(FireballEntity fireball, Entity exclude) {
        Box searchBox = fireball.getBoundingBox().expand(16.0);
        
        List<LivingEntity> potentialTargets = fireball.getWorld()
            .getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                entity -> entity instanceof HostileEntity 
                    && !hitEntities.contains(entity.getUuid())
                    && entity != exclude
                    && entity.isAlive()
            );
        
        // 返回最近的目标
        return potentialTargets.stream()
            .min(Comparator.comparingDouble(
                entity -> entity.squaredDistanceTo(fireball)
            ))
            .orElse(null);
    }
    
    private void redirectFireball(FireballEntity fireball, LivingEntity target) {
        Vec3d toTarget = target.getPos()
            .add(0, target.getHeight() / 2, 0)
            .subtract(fireball.getPos())
            .normalize();
        
        // 设置火球速度方向
        fireball.setVelocity(toTarget.multiply(0.5));
        
        // 生成追踪粒子效果
        if (fireball.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.FLAME,
                fireball.getX(), fireball.getY(), fireball.getZ(),
                2, 0.1, 0.1, 0.1, 0.0
            );
        }
    }
    
    private void spawnHitParticles(World world, Vec3d pos) {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION,
                pos.x, pos.y, pos.z,
                3, 0.2, 0.2, 0.2, 0.0
            );
        }
    }
}
```

---

## 数据流与网络通信

### 客户端-服务器交互流程

#### 1. 打开GUI流程

```
客户端:
  玩家按下Shift+右键 
    ↓
  触发interactMob事件
    ↓
  (在Mixin中) 检测到Shift按下
    ↓
  发送SyncGhastDataPayload (S2C)
    ↓
服务端:
  读取快乐恶魂数据 (HappyGhastData)
    ↓
  打包数据到SyncGhastDataPayload
    ↓
  发送到客户端
    ↓
客户端:
  接收SyncGhastDataPayload
    ↓
  打开HappyGhastScreen
    ↓
  显示GUI界面
```

#### 2. 喂食流程

```
客户端:
  玩家手持食物右键快乐恶魂
    ↓
服务端:
  检测食物类型
    ↓
  计算饱食度和经验值
    ↓
  更新HappyGhastData
    ↓
  检查是否升级
    ↓
  发送反馈消息给玩家
    ↓
  (如果GUI打开) 发送SyncGhastDataPayload更新GUI
```

#### 3. 改名流程

```
客户端:
  玩家在GUI输入框输入名字
    ↓
  输入框失去焦点
    ↓
  发送RenameGhastPayload (C2S)
    ↓
服务端:
  接收改名请求
    ↓
  更新HappyGhastData.customName
    ↓
  设置实体的CustomName
    ↓
  setCustomNameVisible(true)
```

#### 4. 实时数据同步流程

```
客户端 (GUI打开时):
  每10 ticks (0.5秒)
    ↓
  发送RequestGhastDataPayload (C2S)
    ↓
服务端:
  接收请求
    ↓
  读取最新数据
    ↓
  发送SyncGhastDataPayload (S2C)
    ↓
客户端:
  接收数据
    ↓
  更新GUI显示
```

### 网络包设计规范

#### Payload结构示例

```java
public record SyncGhastDataPayload(
    int entityId,
    int level,
    int experience,
    float hunger,
    float maxHealth,
    float currentHealth,
    float maxHunger,
    int expToNext,
    boolean isCreative,
    List<String> favoriteFoods,
    String customName
) implements CustomPayload {
    
    public static final CustomPayload.Id<SyncGhastDataPayload> ID = 
        new CustomPayload.Id<>(Identifier.of("chest-on-ghast", "sync_ghast_data"));
    
    public static final PacketCodec<RegistryByteBuf, SyncGhastDataPayload> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, SyncGhastDataPayload::entityId,
            PacketCodecs.VAR_INT, SyncGhastDataPayload::level,
            PacketCodecs.VAR_INT, SyncGhastDataPayload::experience,
            PacketCodecs.FLOAT, SyncGhastDataPayload::hunger,
            PacketCodecs.FLOAT, SyncGhastDataPayload::maxHealth,
            PacketCodecs.FLOAT, SyncGhastDataPayload::currentHealth,
            PacketCodecs.FLOAT, SyncGhastDataPayload::maxHunger,
            PacketCodecs.VAR_INT, SyncGhastDataPayload::expToNext,
            PacketCodecs.BOOL, SyncGhastDataPayload::isCreative,
            PacketCodecs.STRING.collect(PacketCodecs.toList()), 
            SyncGhastDataPayload::favoriteFoods,
            PacketCodecs.STRING, SyncGhastDataPayload::customName,
            SyncGhastDataPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
```

---

## 配置系统

### JSON配置文件设计

#### 完整配置示例

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
    "3": {
      "maxHealth": 45.0,
      "maxHunger": 400.0,
      "expToNextLevel": 350,
      "hungerDecayMultiplier": 0.81,
      "fireballPower": 3,
      "attackCooldown": 40,
      "enableEffectCloud": true,
      "cloudRadius": 3.0,
      "cloudDuration": 100,
      "damageAmplifier": 0,
      "regenAmplifier": 0
    },
    "6": {
      "maxHealth": 120.0,
      "maxHunger": 3200.0,
      "expToNextLevel": 0,
      "hungerDecayMultiplier": 0.59049,
      "fireballPower": 6,
      "attackCooldown": 15,
      "enableEffectCloud": true,
      "cloudRadius": 5.0,
      "cloudDuration": 160,
      "damageAmplifier": 1,
      "regenAmplifier": 2
    }
  },
  "foodConfig": {
    "snowballHunger": 50.0,
    "snowballExp": 10,
    "favoriteHunger": 80.0,
    "favoriteExp": 20,
    "defaultHunger": 12.0,
    "defaultExp": 5
  },
  "enchantments": {
    "multishot": {
      "enabled": true,
      "requiresLevel": 1,
      "maxLevel": 3
    },
    "duration": {
      "enabled": true,
      "requiresLevel": 3,
      "maxLevel": 3,
      "multipliers": [1.5, 2.0, 3.0]
    },
    "freezing": {
      "enabled": true,
      "requiresLevel": 3,
      "maxLevel": 3,
      "durations": [60, 100, 160],
      "slownessLevels": [5, 7, 10]
    },
    "charm": {
      "enabled": true,
      "requiresLevel": 3,
      "maxLevel": 3,
      "damages": [2.0, 4.0, 6.0],
      "tickInterval": 10
    },
    "gravity": {
      "enabled": true,
      "requiresLevel": 3,
      "maxLevel": 3,
      "ranges": [5.0, 8.0, 12.0],
      "strengths": [0.15, 0.25, 0.40],
      "maxMonsters": 30,
      "maxItems": 50
    },
    "polymorph": {
      "enabled": true,
      "requiresLevel": 3,
      "maxLevel": 3,
      "chances": [0.33, 0.66, 1.0]
    },
    "piercingTracker": {
      "enabled": false,
      "requiresLevel": 1,
      "maxLevel": 3,
      "targets": [2, 3, 5]
    }
  }
}
```

### 配置热重载

```java
public class GhastConfig {
    public void reload() {
        INSTANCE = load();
        Chestonghast.LOGGER.info("配置已重新加载");
    }
    
    // 添加命令支持
    public static void registerReloadCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("ghast")
                    .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> {
                            getInstance().reload();
                            context.getSource().sendFeedback(
                                () -> Text.literal("快乐恶魂配置已重新加载"),
                                true
                            );
                            return 1;
                        })
                    )
            );
        });
    }
}
```

---

## 测试与调试策略

### 单元测试

#### 数据类测试

```java
@Test
public void testLevelUpMechanism() {
    HappyGhastData data = new HappyGhastData();
    
    // 测试初始状态
    assertEquals(1, data.getLevel());
    assertEquals(0, data.getExperience());
    
    // 测试升级
    boolean leveledUp = data.addExperience(100);
    assertTrue(leveledUp);
    assertEquals(2, data.getLevel());
    
    // 测试饱食度恢复
    assertEquals(200.0f, data.getHunger(), 0.01f);
}

@Test
public void testHungerDecay() {
    HappyGhastData data = new HappyGhastData();
    float initialHunger = data.getHunger();
    
    // 模拟1秒后
    Thread.sleep(1000);
    data.updateHunger();
    
    assertTrue(data.getHunger() < initialHunger);
}
```

#### NBT序列化测试

```java
@Test
public void testNbtSerialization() {
    HappyGhastData original = new HappyGhastData(3, 150, 300.0f);
    original.setCustomName("测试恶魂");
    
    // 序列化
    NbtCompound nbt = new NbtCompound();
    original.writeToNbt(nbt);
    
    // 反序列化
    HappyGhastData deserialized = HappyGhastData.readFromNbt(nbt);
    
    assertEquals(original.getLevel(), deserialized.getLevel());
    assertEquals(original.getExperience(), deserialized.getExperience());
    assertEquals(original.getHunger(), deserialized.getHunger(), 0.01f);
    assertEquals(original.getCustomName(), deserialized.getCustomName());
}
```

### 集成测试

#### 服务端测试环境

```java
public class GhastIntegrationTest {
    private MinecraftServer server;
    private ServerWorld world;
    
    @BeforeEach
    public void setup() {
        // 创建测试服务器
        server = createTestServer();
        world = server.getOverworld();
    }
    
    @Test
    public void testGhastSpawnAndInteraction() {
        // 生成快乐恶魂
        HappyGhastEntity ghast = EntityType.HAPPY_GHAST.create(world);
        world.spawnEntity(ghast);
        
        // 创建测试玩家
        ServerPlayerEntity player = createTestPlayer();
        
        // 测试喂食
        ItemStack food = new ItemStack(Items.APPLE);
        ActionResult result = ghast.interactMob(player, Hand.MAIN_HAND);
        
        assertEquals(ActionResult.SUCCESS, result);
        assertTrue(getGhastData(ghast).getExperience() > 0);
    }
}
```

### 调试工具

#### 调试命令

```java
public static void registerDebugCommands() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        dispatcher.register(
            CommandManager.literal("ghast")
                .then(CommandManager.literal("debug")
                    .requires(source -> source.hasPermissionLevel(2))
                    
                    // 设置等级
                    .then(CommandManager.literal("setLevel")
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 6))
                            .executes(context -> {
                                int level = IntegerArgumentType.getInteger(context, "level");
                                HappyGhastEntity ghast = getTargetGhast(context.getSource());
                                if (ghast != null) {
                                    setGhastLevel(ghast, level);
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("等级已设置为: " + level),
                                        false
                                    );
                                }
                                return 1;
                            })
                        )
                    )
                    
                    // 添加经验
                    .then(CommandManager.literal("addExp")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                HappyGhastEntity ghast = getTargetGhast(context.getSource());
                                if (ghast != null) {
                                    addGhastExp(ghast, amount);
                                    context.getSource().sendFeedback(
                                        () -> Text.literal("已添加经验: " + amount),
                                        false
                                    );
                                }
                                return 1;
                            })
                        )
                    )
                    
                    // 查看数据
                    .then(CommandManager.literal("info")
                        .executes(context -> {
                            HappyGhastEntity ghast = getTargetGhast(context.getSource());
                            if (ghast != null) {
                                HappyGhastData data = getGhastData(ghast);
                                context.getSource().sendFeedback(
                                    () -> Text.literal(String.format(
                                        "等级: %d, 经验: %d/%d, 饱食度: %.1f/%.1f, 血量: %.1f/%.1f",
                                        data.getLevel(),
                                        data.getExperience(),
                                        data.getExpToNextLevel(),
                                        data.getHunger(),
                                        data.getMaxHunger(),
                                        ghast.getHealth(),
                                        data.getMaxHealth()
                                    )),
                                    false
                                );
                            }
                            return 1;
                        })
                    )
                )
        );
    });
}
```

---

## 性能优化建议

### 1. 网络包优化

**问题**: 频繁的网络包发送会增加带宽消耗

**解决方案**:
```java
// 使用批量更新而非单个更新
private int syncCooldown = 0;

@Override
public void tick() {
    syncCooldown--;
    if (syncCooldown <= 0 && needsSync()) {
        sendSyncPacket();
        syncCooldown = 10; // 0.5秒一次
    }
}
```

### 2. 粒子效果优化

**问题**: 大量粒子会降低客户端FPS

**解决方案**:
```java
// 根据玩家距离调整粒子数量
private int getParticleCount(ServerWorld world, Vec3d pos) {
    int nearbyPlayers = world.getPlayers(
        player -> player.squaredDistanceTo(pos) < 1600 // 40格内
    ).size();
    
    return Math.max(5, 30 - nearbyPlayers * 5);
}

// 使用粒子LOD（细节层次）
if (distanceToPlayer < 16) {
    spawnParticles(30); // 近距离：高质量
} else if (distanceToPlayer < 32) {
    spawnParticles(15); // 中距离：中质量
} else {
    spawnParticles(5);  // 远距离：低质量
}
```

### 3. AI优化

**问题**: 复杂的AI计算会影响服务器TPS

**解决方案**:
```java
// 使用更新间隔
private int aiUpdateInterval = 10;
private int aiTick = 0;

@Override
public void tick() {
    aiTick++;
    if (aiTick >= aiUpdateInterval) {
        aiTick = 0;
        updateAI(); // 每10 ticks更新一次AI
    }
}

// 限制寻找目标的范围
private LivingEntity findTarget() {
    // 只在16格内寻找，避免大范围搜索
    return world.getClosestEntity(
        HostileEntity.class,
        TargetPredicate.createAttackable().setBaseMaxDistance(16.0),
        ghast, x, y, z,
        ghast.getBoundingBox().expand(16.0)
    );
}
```

### 4. 效果云优化

**问题**: 引力奇点附魔会对大量实体进行计算

**解决方案**:
```java
// 限制最大影响实体数量
private static final int MAX_GRAVITY_MONSTERS = 30;
private static final int MAX_GRAVITY_ITEMS = 50;

// 使用空间分区
private List<Entity> getEntitiesInRange(Vec3d center, float range) {
    // 使用Box限制搜索范围
    Box searchBox = Box.of(center, range * 2, range * 2, range * 2);
    
    return world.getOtherEntities(null, searchBox, entity -> {
        // 早期过滤
        if (!entity.isAlive()) return false;
        if (entity.squaredDistanceTo(center) > range * range) return false;
        return entity instanceof HostileEntity || entity instanceof ItemEntity;
    }).stream()
        .limit(MAX_GRAVITY_MONSTERS + MAX_GRAVITY_ITEMS)
        .toList();
}
```

### 5. 数据存储优化

**问题**: NBT读写可能造成卡顿

**解决方案**:
```java
// 使用缓存避免频繁NBT操作
private HappyGhastData cachedData;
private boolean dataDirty = false;

public HappyGhastData getData() {
    if (cachedData == null) {
        cachedData = loadFromNbt();
    }
    return cachedData;
}

public void markDirty() {
    dataDirty = true;
}

// 批量保存
public void tick() {
    if (dataDirty && tickCounter % 100 == 0) {
        saveToNbt();
        dataDirty = false;
    }
}
```

---

## 扩展性设计

### 1. 插件式附魔系统

```java
public interface EnchantmentEffect {
    String getId();
    int getMaxLevel();
    int getRequiredGhastLevel();
    boolean canApply(HappyGhastEntity ghast);
    void onFireballShoot(FireballEntity fireball, HappyGhastEntity ghast);
    void onFireballHit(FireballEntity fireball, HitResult hitResult);
    void onEffectCloudTick(AreaEffectCloudEntity cloud);
}

public class EnchantmentRegistry {
    private static final Map<String, EnchantmentEffect> EFFECTS = new HashMap<>();
    
    public static void register(EnchantmentEffect effect) {
        EFFECTS.put(effect.getId(), effect);
    }
    
    public static EnchantmentEffect get(String id) {
        return EFFECTS.get(id);
    }
}

// 使用示例：添加新附魔
public class CustomEnchantment implements EnchantmentEffect {
    @Override
    public String getId() {
        return "custom_enchantment";
    }
    
    @Override
    public void onFireballShoot(FireballEntity fireball, HappyGhastEntity ghast) {
        // 自定义逻辑
    }
}
```

### 2. 事件系统

```java
public class GhastEvents {
    public static final Event<LevelUpCallback> LEVEL_UP = 
        EventFactory.createArrayBacked(LevelUpCallback.class, callbacks -> (ghast, newLevel) -> {
            for (LevelUpCallback callback : callbacks) {
                callback.onLevelUp(ghast, newLevel);
            }
        });
    
    public static final Event<FeedCallback> FEED = 
        EventFactory.createArrayBacked(FeedCallback.class, callbacks -> (ghast, food, player) -> {
            for (FeedCallback callback : callbacks) {
                ActionResult result = callback.onFeed(ghast, food, player);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        });
    
    @FunctionalInterface
    public interface LevelUpCallback {
        void onLevelUp(HappyGhastEntity ghast, int newLevel);
    }
    
    @FunctionalInterface
    public interface FeedCallback {
        ActionResult onFeed(HappyGhastEntity ghast, ItemStack food, PlayerEntity player);
    }
}

// 使用示例
GhastEvents.LEVEL_UP.register((ghast, newLevel) -> {
    if (newLevel == 6) {
        // 达到满级时的特殊奖励
        spawnFireworks(ghast.getPos());
    }
});
```

### 3. 模块化配置

```java
public abstract class ConfigModule {
    protected final String moduleName;
    
    public ConfigModule(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public abstract void loadConfig(JsonObject json);
    public abstract JsonObject saveConfig();
    public abstract void validate() throws ConfigException;
}

public class LevelConfigModule extends ConfigModule {
    private Map<Integer, LevelData> levels;
    
    @Override
    public void loadConfig(JsonObject json) {
        // 加载等级配置
    }
}

public class EnchantmentConfigModule extends ConfigModule {
    private Map<String, EnchantmentConfig> enchantments;
    
    @Override
    public void loadConfig(JsonObject json) {
        // 加载附魔配置
    }
}

// 配置管理器
public class ConfigManager {
    private final List<ConfigModule> modules = new ArrayList<>();
    
    public void registerModule(ConfigModule module) {
        modules.add(module);
    }
    
    public void loadAll() {
        for (ConfigModule module : modules) {
            module.loadConfig(getModuleJson(module.moduleName));
            module.validate();
        }
    }
}
```

### 4. API接口

```java
public class ChestOnGhastAPI {
    private static final String VERSION = "1.0.0";
    
    /**
     * 获取快乐恶魂的数据
     */
    public static HappyGhastData getData(HappyGhastEntity ghast) {
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            return accessor.getGhastData();
        }
        throw new IllegalArgumentException("Invalid ghast entity");
    }
    
    /**
     * 为快乐恶魂添加经验
     */
    public static boolean addExperience(HappyGhastEntity ghast, int amount) {
        HappyGhastData data = getData(ghast);
        return data.addExperience(amount);
    }
    
    /**
     * 检查快乐恶魂是否有指定附魔
     */
    public static boolean hasEnchantment(HappyGhastEntity ghast, String enchantmentId) {
        HappyGhastData data = getData(ghast);
        return data.getEnchantments().containsKey(enchantmentId);
    }
    
    /**
     * 注册自定义附魔效果
     */
    public static void registerEnchantment(EnchantmentEffect effect) {
        EnchantmentRegistry.register(effect);
    }
    
    /**
     * 获取API版本
     */
    public static String getVersion() {
        return VERSION;
    }
}
```

---

## 总结

### 已实现功能清单

- ✅ 6级等级系统
- ✅ 饱食度系统（自动降低）
- ✅ 喂食系统（经验值和饱食度恢复）
- ✅ GUI界面（实时数据显示）
- ✅ 名字自定义
- ✅ 最喜欢的食物系统
- ✅ 配置文件系统
- ✅ 网络同步

### 待实现功能清单

- ❌ 自动战斗AI
- ❌ 效果云系统（3级解锁）
- ❌ 瞄准镜物品
- ❌ 附魔系统（6种附魔）
- ❌ 附魔GUI界面
- ❌ 穿透追踪附魔

### 技术债务

1. **NBT持久化**: 当前Mixin中的NBT保存/加载需要完善
2. **AI系统**: 战斗AI需要完整实现
3. **性能测试**: 需要大规模测试（100+快乐恶魂）
4. **多人测试**: 需要测试多玩家同时交互

### 开发优先级建议

#### 第一阶段（核心功能）
1. 完善NBT持久化
2. 实现战斗AI系统
3. 实现效果云系统

#### 第二阶段（扩展功能）
4. 实现瞄准镜物品
5. 实现附魔系统框架
6. 实现连射、持久、冰冻附魔

#### 第三阶段（高级功能）
7. 实现魅惑、引力奇点、变形附魔
8. 实现附魔GUI
9. 实现穿透追踪附魔

#### 第四阶段（优化与扩展）
10. 性能优化
11. API开发
12. 文档完善

---

## 参考资源

### Fabric官方文档
- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Fabric API Javadoc](https://maven.fabricmc.net/docs/fabric-api-0.117.3+1.21.9/)
- [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod)

### Minecraft开发资源
- [Minecraft Wiki](https://minecraft.fandom.com/)
- [Yarn Mappings](https://github.com/FabricMC/yarn)
- [Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki)

### 社区资源
- [Fabric Discord](https://discord.gg/v6v4pMv)
- [Fabricord](https://discord.gg/v6v4pMv)
- [MMD Discord](https://discord.gg/mmd)

---

**文档版本**: 1.0.0  
**创建日期**: 2025-11-16  
**最后更新**: 2025-11-16  
**维护者**: Chest on Ghast Mod Team

---

*本文档基于FEATURES_GUIDE.md和现有代码库创建，旨在为开发者提供系统化的技术实现指导。*
