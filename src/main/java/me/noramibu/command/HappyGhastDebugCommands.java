package me.noramibu.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.noramibu.Chestonghast;
import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.element.GhastElement;
import me.noramibu.level.LevelConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * 调试命令注册器
 * 提供快速生成指定属性快乐恶魂以及在其脚下生成怪物群的便捷命令，
 * 方便在调试阶段复现战斗场景。
 */
public final class HappyGhastDebugCommands {
	private static final SimpleCommandExceptionType FAILED_GHAST_SPAWN = new SimpleCommandExceptionType(Text.literal("无法生成快乐恶魂，稍后再试。"));
	private static final SimpleCommandExceptionType NO_GHAST_FOUND = new SimpleCommandExceptionType(Text.literal("附近没有找到快乐恶魂。"));

	// 预定义的怪物池，混合常见近战与远程威胁，便于覆盖各种战斗表现
	private static final List<EntityType<? extends HostileEntity>> HOSTILE_POOL = List.of(
		EntityType.ZOMBIE,
		EntityType.HUSK,
		EntityType.DROWNED,
		EntityType.SPIDER,
		EntityType.CAVE_SPIDER,
		EntityType.SKELETON,
		EntityType.STRAY
	);

	private static final SuggestionProvider<ServerCommandSource> ELEMENT_SUGGESTIONS = HappyGhastDebugCommands::suggestElements;

	private HappyGhastDebugCommands() {
	}

	/**
	 * 对外暴露统一的注册入口，在模组初始化阶段调用即可，
	 * 以确保命令在服务端🟢加载且拥有最新的注册表上下文。
	 */
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("ghastdebug")
				.requires(source -> source.hasPermissionLevel(2))
				.then(createSpawnCommand())
				.then(createWaveCommand())
				.then(createAttachCommand());

			dispatcher.register(root);
		});
	}

	/**
	 * `/ghastdebug spawn <element> [level] [hostiles]`
	 * 生成指定属性、等级的快乐恶魂，可选立即刷新一批敌对生物以测试战斗。
	 */
	private static ArgumentBuilder<ServerCommandSource, ?> createSpawnCommand() {
		return CommandManager.literal("spawn")
			.then(CommandManager.argument("element", com.mojang.brigadier.arguments.StringArgumentType.word())
				.suggests(ELEMENT_SUGGESTIONS)
				.executes(ctx -> spawnGhast(ctx, 1, 0))
				.then(CommandManager.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, LevelConfig.MAX_LEVEL))
					.executes(ctx -> spawnGhast(ctx, ctx.getArgument("level", Integer.class), 0))
					.then(CommandManager.argument("hostiles", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 40))
						.executes(ctx -> spawnGhast(ctx, ctx.getArgument("level", Integer.class), ctx.getArgument("hostiles", Integer.class))))));
	}

	/**
	 * `/ghastdebug hostiles <count>`
	 * 在距离执行者最近的快乐恶魂周围生成指定数量的敌对生物。
	 */
	private static ArgumentBuilder<ServerCommandSource, ?> createWaveCommand() {
		return CommandManager.literal("hostiles")
			.then(CommandManager.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 40))
				.executes(ctx -> spawnHostileWave(ctx.getSource(), ctx.getArgument("count", Integer.class), null))
				.then(CommandManager.argument("target", EntityArgumentType.entity())
					.executes(ctx -> {
						Entity rawTarget = EntityArgumentType.getEntity(ctx, "target");
						if (rawTarget instanceof HappyGhastEntity ghastEntity) {
							return spawnHostileWave(ctx.getSource(), ctx.getArgument("count", Integer.class), ghastEntity);
						}
						throw NO_GHAST_FOUND.create();
					})));
	}

	/**
	 * `/ghastdebug attach <selector>`
	 * 让指定怪物立即把目标锁定为最近的快乐恶魂，帮助检验仇恨转移逻辑。
	 */
	private static ArgumentBuilder<ServerCommandSource, ?> createAttachCommand() {
		return CommandManager.literal("attach")
			.then(CommandManager.argument("mob", EntityArgumentType.entity())
				.executes(ctx -> {
					Entity raw = EntityArgumentType.getEntity(ctx, "mob");
					if (!(raw instanceof HostileEntity monster)) {
						throw new SimpleCommandExceptionType(Text.literal("只能为敌对生物设置目标。")).create();
					}
					ServerWorld world = ctx.getSource().getWorld();
					Vec3d rawPos = new Vec3d(raw.getX(), raw.getBodyY(0.5), raw.getZ());
					HappyGhastEntity anchor = findNearestGhast(world, rawPos, 32.0);
					if (anchor == null) {
						throw NO_GHAST_FOUND.create();
					}
					monster.setTarget(anchor);
					ctx.getSource().sendFeedback(() -> Text.literal(String.format(Locale.ROOT, "已让 %s 追击 %s", monster.getDisplayName().getString(), anchor.getDisplayName().getString())), true);
					return 1;
				}));
	}

	private static CompletableFuture<Suggestions> suggestElements(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Arrays.stream(GhastElement.values()).map(GhastElement::getId), builder);
	}

	private static int spawnGhast(CommandContext<ServerCommandSource> ctx, int requestedLevel, int hostileCount) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerWorld world = source.getWorld();
		GhastElement element = GhastElement.fromId(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "element"));

		HappyGhastEntity ghast = EntityType.HAPPY_GHAST.create(world, SpawnReason.COMMAND);
		if (ghast == null) {
			throw FAILED_GHAST_SPAWN.create();
		}

		Vec3d spawnPos = source.getPosition();
		ghast.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.random.nextFloat() * 360.0f, 0.0f);

		HappyGhastDataAccessor accessor = (HappyGhastDataAccessor) ghast;
		HappyGhastData debugData = createDebugData(element, requestedLevel);
		accessor.setGhastData(debugData);
		ghast.setCustomName(Text.literal(String.format(Locale.ROOT, "[调试]%s (%s)", debugData.getCustomName(), element.getId().toUpperCase(Locale.ROOT))));
		ghast.setCustomNameVisible(true);
		ghast.setHealth(debugData.getMaxHealth());

		world.spawnEntity(ghast);
		Chestonghast.LOGGER.info("GhastDebug: spawned {} ghast for {}", element.getId(), source.getName());

		if (hostileCount > 0) {
			spawnHostilesAround(ghast, hostileCount);
		}

		source.sendFeedback(() -> Text.literal(String.format(Locale.ROOT, "生成 %s 属性快乐恶魂 (等级 %d)%s", element.getId(), debugData.getLevel(), hostileCount > 0 ? " 并刷出怪物群" : "")), true);
		return 1;
	}

	private static HappyGhastData createDebugData(GhastElement element, int requestedLevel) {
		int level = MathHelper.clamp(requestedLevel, 1, LevelConfig.MAX_LEVEL);
		HappyGhastData data = new HappyGhastData();
		while (data.getLevel() < level) {
			int need = Math.max(1, data.getExpToNextLevel());
			data.addExperience(need);
		}
		data.setElement(element);
		data.setCustomName(String.format(Locale.ROOT, "快乐恶魂·%s", element.getId().toUpperCase(Locale.ROOT)));
		return data;
	}

	private static int spawnHostileWave(ServerCommandSource source, int count, HappyGhastEntity optionalAnchor) throws CommandSyntaxException {
		ServerWorld world = source.getWorld();
		HappyGhastEntity anchor = optionalAnchor != null ? optionalAnchor : findNearestGhast(world, source.getPosition(), 48.0);
		if (anchor == null) {
			throw NO_GHAST_FOUND.create();
		}

		int spawned = spawnHostilesAround(anchor, count);
		source.sendFeedback(() -> Text.literal(String.format(Locale.ROOT, "已在 %s 周围生成 %d 个怪物", anchor.getDisplayName().getString(), spawned)), true);
		return spawned;
	}

	private static int spawnHostilesAround(HappyGhastEntity anchor, int count) {
		World world = anchor.getEntityWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return 0;
		}

		int spawned = 0;
		for (int i = 0; i < count; i++) {
			EntityType<? extends HostileEntity> type = HOSTILE_POOL.get(serverWorld.random.nextInt(HOSTILE_POOL.size()));
			HostileEntity monster = type.create(serverWorld, SpawnReason.COMMAND);
			if (monster == null) {
				continue;
			}

			double angle = serverWorld.random.nextDouble() * Math.PI * 2;
			double radius = 1.5 + serverWorld.random.nextDouble() * 3.0;
			double x = anchor.getX() + Math.cos(angle) * radius;
			double z = anchor.getZ() + Math.sin(angle) * radius;
			double y = anchor.getY();
			monster.refreshPositionAndAngles(x, y, z, serverWorld.random.nextFloat() * 360.0f, 0.0f);
			serverWorld.spawnEntity(monster);
			monster.setTarget(anchor);
			spawned++;
		}
		Chestonghast.LOGGER.info("GhastDebug: spawned {} hostiles near {}", spawned, anchor.getDisplayName().getString());
		return spawned;
	}

	private static HappyGhastEntity findNearestGhast(ServerWorld world, Vec3d origin, double radius) {
		Box searchBox = new Box(origin.x - radius, origin.y - radius, origin.z - radius, origin.x + radius, origin.y + radius, origin.z + radius);
		return world.getEntitiesByClass(HappyGhastEntity.class, searchBox, Entity::isAlive)
			.stream()
			.min((a, b) -> Double.compare(a.squaredDistanceTo(origin), b.squaredDistanceTo(origin)))
			.orElse(null);
	}

}
