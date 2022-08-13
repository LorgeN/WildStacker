package com.bgsoftware.wildstacker.nms.v1_12_R1;

import com.bgsoftware.common.reflection.ReflectField;
import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.enums.SpawnCause;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.bgsoftware.wildstacker.api.upgrades.SpawnerUpgrade;
import com.bgsoftware.wildstacker.listeners.EntitiesListener;
import com.bgsoftware.wildstacker.objects.WStackedEntity;
import com.bgsoftware.wildstacker.objects.WStackedItem;
import com.bgsoftware.wildstacker.utils.chunks.ChunkPosition;
import com.bgsoftware.wildstacker.utils.spawners.SpawnerCachedData;
import com.bgsoftware.wildstacker.utils.spawners.SyncedCreatureSpawner;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.BlockRotatable;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.CriterionTriggers;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.EnchantmentManager;
import net.minecraft.server.v1_12_R1.Enchantments;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityAnimal;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.EntityArrow;
import net.minecraft.server.v1_12_R1.EntityFireball;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EntityItem;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.EntityTracker;
import net.minecraft.server.v1_12_R1.EntityTypes;
import net.minecraft.server.v1_12_R1.EntityVillager;
import net.minecraft.server.v1_12_R1.EntityZombieVillager;
import net.minecraft.server.v1_12_R1.EnumHand;
import net.minecraft.server.v1_12_R1.EnumItemSlot;
import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.IWorldAccess;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.ItemSword;
import net.minecraft.server.v1_12_R1.Items;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.MobEffect;
import net.minecraft.server.v1_12_R1.MobEffects;
import net.minecraft.server.v1_12_R1.MobSpawnerAbstract;
import net.minecraft.server.v1_12_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.PacketPlayOutCollect;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_12_R1.SoundCategory;
import net.minecraft.server.v1_12_R1.SoundEffect;
import net.minecraft.server.v1_12_R1.StatisticList;
import net.minecraft.server.v1_12_R1.TileEntityMobSpawner;
import net.minecraft.server.v1_12_R1.World;
import net.minecraft.server.v1_12_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftAnimals;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftChicken;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftVehicle;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.EntityEquipment;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSAdapter implements com.bgsoftware.wildstacker.nms.NMSAdapter {

    private static final ReflectField<Integer> ENTITY_EXP = new ReflectField<>(EntityInsentient.class, int.class, "b_");
    private static final ReflectField<Integer> LAST_DAMAGE_BY_PLAYER_TIME = new ReflectField<>(EntityLiving.class, int.class, "lastDamageByPlayerTime");
    private static final ReflectField<Boolean> FROM_MOB_SPAWNER = new ReflectField<>(Entity.class, boolean.class, "fromMobSpawner");
    private static final ReflectField<Collection[]> ENTITY_SLICES = new ReflectField<>(Chunk.class, Collection[].class, "entitySlices");
    private static final ReflectMethod<Boolean> ALWAYS_GIVES_EXP = new ReflectMethod<>(EntityLiving.class, "alwaysGivesExp");
    private static final ReflectMethod<Boolean> IS_DROP_EXPERIENCE = new ReflectMethod<>(EntityLiving.class, "isDropExperience");
    private static final ReflectMethod<SoundEffect> GET_SOUND_DEATH = new ReflectMethod<>(EntityLiving.class, "cf");
    private static final ReflectMethod<Float> GET_SOUND_VOLUME = new ReflectMethod<>(EntityLiving.class, "cq");
    private static final ReflectMethod<Float> GET_SOUND_PITCH = new ReflectMethod<>(EntityLiving.class, "cr");
    private static final ReflectField<Boolean> ENTITY_FORCE_DROPS = new ReflectField<>(EntityLiving.class, Boolean.class, "forceDrops");

    /*
     *   Entity methods
     */

    @Override
    public String getMappingsHash() {
        return null;
    }

    @Override
    public <T extends org.bukkit.entity.Entity> T createEntity(Location location, Class<T> type, SpawnCause spawnCause, Consumer<T> beforeSpawnConsumer, Consumer<T> afterSpawnConsumer) {
        CraftWorld world = (CraftWorld) location.getWorld();

        Entity nmsEntity = world.createEntity(location, type);
        org.bukkit.entity.Entity bukkitEntity = nmsEntity.getBukkitEntity();

        if (beforeSpawnConsumer != null) {
            //noinspection unchecked
            beforeSpawnConsumer.accept((T) bukkitEntity);
        }

        world.addEntity(nmsEntity, spawnCause.toSpawnReason());

        WStackedEntity.of(bukkitEntity).setSpawnCause(spawnCause);

        if (afterSpawnConsumer != null) {
            //noinspection unchecked
            afterSpawnConsumer.accept((T) bukkitEntity);
        }

        return type.cast(bukkitEntity);
    }

    @Override
    public <T extends org.bukkit.entity.Entity> T spawnEntity(Location location, Class<T> type, SpawnCause spawnCause) {
        CraftWorld world = (CraftWorld) location.getWorld();

        assert world != null;

        Entity nmsEntity = world.createEntity(location, type);
        org.bukkit.entity.Entity bukkitEntity = nmsEntity.getBukkitEntity();

        world.addEntity(nmsEntity, spawnCause.toSpawnReason());

        return type.cast(bukkitEntity);
    }

    @Override
    public Zombie spawnZombieVillager(Villager villager) {
        EntityVillager entityVillager = ((CraftVillager) villager).getHandle();
        EntityZombieVillager entityZombieVillager = new EntityZombieVillager(entityVillager.world);

        entityZombieVillager.u(entityVillager);
        entityZombieVillager.setProfession(entityVillager.getProfession());
        entityZombieVillager.setBaby(entityVillager.isBaby());
        entityZombieVillager.setNoAI(entityVillager.isNoAI());

        if (entityVillager.hasCustomName()) {
            entityZombieVillager.setCustomName(entityVillager.getCustomName());
            entityZombieVillager.setCustomNameVisible(entityVillager.getCustomNameVisible());
        }

        entityVillager.world.addEntity(entityZombieVillager, CreatureSpawnEvent.SpawnReason.INFECTION);
        entityVillager.world.a(null, 1026, new BlockPosition(entityVillager), 0);

        return (Zombie) entityZombieVillager.getBukkitEntity();
    }

    @Override
    public void setInLove(Animals entity, Player breeder, boolean inLove) {
        EntityAnimal nmsEntity = ((CraftAnimals) entity).getHandle();
        EntityPlayer entityPlayer = ((CraftPlayer) breeder).getHandle();
        if (inLove)
            nmsEntity.f(entityPlayer);
        else
            nmsEntity.resetLove();
    }

    @Override
    public boolean isInLove(Animals entity) {
        return ((EntityAnimal) ((CraftEntity) entity).getHandle()).isInLove();
    }

    @Override
    public boolean isAnimalFood(Animals animal, org.bukkit.inventory.ItemStack itemStack) {
        EntityAnimal nmsEntity = ((CraftAnimals) animal).getHandle();
        return itemStack != null && nmsEntity.e(CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public int getEntityExp(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();

        if (!(entityLiving instanceof EntityInsentient))
            return 0;

        EntityInsentient entityInsentient = (EntityInsentient) entityLiving;

        int defaultEntityExp = ENTITY_EXP.get(entityInsentient);
        int exp = entityInsentient.getExpReward();

        ENTITY_EXP.set(entityInsentient, defaultEntityExp);

        return exp;
    }

    @Override
    public boolean canDropExp(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        int lastDamageByPlayerTime = LAST_DAMAGE_BY_PLAYER_TIME.get(entityLiving);
        boolean alwaysGivesExp = ALWAYS_GIVES_EXP.invoke(entityLiving);
        boolean isDropExperience = IS_DROP_EXPERIENCE.invoke(entityLiving);
        return (lastDamageByPlayerTime > 0 || alwaysGivesExp) && isDropExperience &&
                entityLiving.world.getGameRules().getBoolean("doMobLoot");
    }

    @Override
    public void updateLastDamageTime(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        LAST_DAMAGE_BY_PLAYER_TIME.set(entityLiving, 100);
    }

    @Override
    public void setHealthDirectly(LivingEntity livingEntity, double health) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        entityLiving.setHealth((float) health);
    }

    @Override
    public void setEntityDead(LivingEntity livingEntity, boolean dead) {
        ((CraftLivingEntity) livingEntity).getHandle().dead = dead;
    }

    @Override
    public int getEggLayTime(Chicken chicken) {
        return ((CraftChicken) chicken).getHandle().bD;
    }

    @Override
    public void setNerfedEntity(LivingEntity livingEntity, boolean nerfed) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        entityLiving.fromMobSpawner = nerfed;
    }

    @Override
    public void setKiller(LivingEntity livingEntity, Player killer) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        entityLiving.killer = killer == null ? null : ((CraftPlayer) killer).getHandle();
    }

    @Override
    public boolean canSpawnOn(org.bukkit.entity.Entity bukkitEntity, Location location) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        Entity entity = EntityTypes.a(((CraftEntity) bukkitEntity).getHandle().getClass(), world);

        if (entity == null) {
            WildStackerPlugin.log("Failed to get entity type from " + bukkitEntity.getType());
            return true;
        }

        entity.setPosition(location.getX(), location.getY(), location.getZ());
        return !(entity instanceof EntityInsentient) || (((EntityInsentient) entity).P() && ((EntityInsentient) entity).canSpawn());
    }

    @Override
    public Collection<org.bukkit.entity.Entity> getEntitiesAtChunk(ChunkPosition chunkPosition) {
        World world = ((CraftWorld) Bukkit.getWorld(chunkPosition.getWorld())).getHandle();

        Chunk chunk = world.getChunkIfLoaded(chunkPosition.getX(), chunkPosition.getZ());

        if (chunk == null)
            return new ArrayList<>();

        return Arrays.stream(chunk.entitySlices)
                .flatMap(Collection::stream)
                .map(Entity::getBukkitEntity)
                .collect(Collectors.toList());
    }

    @Override
    public float getItemInMainHandDropChance(EntityEquipment entityEquipment) {
        return entityEquipment.getItemInMainHandDropChance();
    }

    @Override
    public float getItemInOffHandDropChance(EntityEquipment entityEquipment) {
        return entityEquipment.getItemInOffHandDropChance();
    }

    @Override
    public void setItemInMainHand(EntityEquipment entityEquipment, org.bukkit.inventory.ItemStack itemStack) {
        entityEquipment.setItemInMainHand(itemStack);
    }

    @Override
    public void setItemInOffHand(EntityEquipment entityEquipment, org.bukkit.inventory.ItemStack itemStack) {
        entityEquipment.setItemInOffHand(itemStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInOffHand(EntityEquipment entityEquipment) {
        return entityEquipment.getItemInOffHand();
    }

    @Override
    public boolean shouldArmorBeDamaged(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        return nmsItem != null && nmsItem.f();
    }

    @Override
    public void handleSweepingEdge(Player attacker, org.bukkit.inventory.ItemStack usedItem, LivingEntity target, double damage) {
        EntityLiving targetLiving = ((CraftLivingEntity) target).getHandle();
        EntityHuman entityHuman = ((CraftPlayer) attacker).getHandle();

        // Making sure the player used a sword.
        if (usedItem.getType() == Material.AIR || !(CraftItemStack.asNMSCopy(usedItem).getItem() instanceof ItemSword))
            return;

        float sweepDamage = 1.0F + EnchantmentManager.a(entityHuman) * (float) damage;
        List<EntityLiving> nearbyEntities = targetLiving.world.a(EntityLiving.class, targetLiving.getBoundingBox().grow(1.0D, 0.25D, 1.0D));

        for (EntityLiving nearby : nearbyEntities) {
            if (nearby != targetLiving && nearby != entityHuman && !entityHuman.r(nearby) && (!(nearby instanceof EntityArmorStand) || !((EntityArmorStand) nearby).isMarker()) && entityHuman.h(nearby) < 9.0D) {
                nearby.damageEntity(DamageSource.playerAttack(entityHuman).sweep(), sweepDamage);
            }
        }
    }

    @Override
    public boolean handleTotemOfUndying(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();

        ItemStack totemOfUndying = ItemStack.a;

        for (EnumHand enumHand : EnumHand.values()) {
            ItemStack handItem = entityLiving.b(enumHand);
            if (handItem.getItem() == Items.cY) {
                totemOfUndying = handItem;
                break;
            }
        }

        EntityResurrectEvent event = new EntityResurrectEvent(livingEntity);
        event.setCancelled(totemOfUndying.isEmpty());
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return false;

        if (!totemOfUndying.isEmpty()) {
            totemOfUndying.subtract(1);

            if (entityLiving instanceof EntityPlayer) {
                ((EntityPlayer) entityLiving).b(StatisticList.b(Items.cY));
                CriterionTriggers.A.a((EntityPlayer) entityLiving, totemOfUndying);
            }

            entityLiving.setHealth(1.0F);
            entityLiving.removeAllEffects();
            entityLiving.addEffect(new MobEffect(MobEffects.REGENERATION, 900, 1));
            entityLiving.addEffect(new MobEffect(MobEffects.ABSORBTION, 100, 1));
            entityLiving.addEffect(new MobEffect(MobEffects.FIRE_RESISTANCE, 800, 0));
            entityLiving.world.broadcastEntityEffect(entityLiving, (byte) 35);
        }

        return true;
    }

    /*
     *   Spawner methods
     */

    @Override
    public SyncedCreatureSpawner createSyncedSpawner(CreatureSpawner creatureSpawner) {
        return new SyncedCreatureSpawnerImpl(creatureSpawner.getBlock());
    }

    @Override
    public boolean isRotatable(Block block) {
        World world = ((CraftWorld) block.getWorld()).getHandle();
        return world.getType(new BlockPosition(block.getX(), block.getY(), block.getZ())).getBlock() instanceof BlockRotatable;
    }

    /*
     *   Item methods
     */

    @Override
    public StackedItem createItem(Location location, org.bukkit.inventory.ItemStack itemStack, SpawnCause spawnCause, Consumer<StackedItem> itemConsumer) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();

        EntityItem entityItem = new EntityItem(craftWorld.getHandle(), location.getX(), location.getY(), location.getZ(), CraftItemStack.asNMSCopy(itemStack));

        entityItem.pickupDelay = 10;

        StackedItem stackedItem = WStackedItem.ofBypass((Item) entityItem.getBukkitEntity());

        itemConsumer.accept(stackedItem);

        craftWorld.addEntity(entityItem, spawnCause.toSpawnReason());

        return stackedItem;
    }

    @Override
    public Enchantment getGlowEnchant() {
        return new Enchantment(101) {
            @Override
            public String getName() {
                return "WildStackerGlow";
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public int getStartLevel() {
                return 0;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return null;
            }

            @Override
            public boolean isTreasure() {
                return false;
            }

            @Override
            public boolean isCursed() {
                return false;
            }

            @Override
            public boolean conflictsWith(Enchantment enchantment) {
                return false;
            }

            @Override
            public boolean canEnchantItem(org.bukkit.inventory.ItemStack itemStack) {
                return true;
            }
        };
    }

    @Override
    public org.bukkit.inventory.ItemStack getPlayerSkull(org.bukkit.inventory.ItemStack bukkitItem, String texture) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        NBTTagCompound nbtTagCompound = itemStack.getTag() != null ? itemStack.getTag() : new NBTTagCompound();

        NBTTagCompound skullOwner = nbtTagCompound.hasKey("SkullOwner") ? nbtTagCompound.getCompound("SkullOwner") : new NBTTagCompound();

        skullOwner.setString("Id", new UUID(texture.hashCode(), texture.hashCode()).toString());

        NBTTagCompound properties = new NBTTagCompound();

        NBTTagList textures = new NBTTagList();
        NBTTagCompound signature = new NBTTagCompound();
        signature.setString("Value", texture);
        textures.add(signature);

        properties.set("textures", textures);

        skullOwner.set("Properties", properties);

        nbtTagCompound.set("SkullOwner", skullOwner);

        itemStack.setTag(nbtTagCompound);

        return CraftItemStack.asBukkitCopy(itemStack);
    }

    @Override
    public void awardKillScore(org.bukkit.entity.Entity bukkitDamaged,
                               org.bukkit.entity.Entity damagerEntity) {
        Entity damaged = ((CraftEntity) bukkitDamaged).getHandle();
        Entity damager = ((CraftEntity) damagerEntity).getHandle();

        DamageSource damageSource = null;

        if (damager instanceof EntityHuman) {
            damageSource = DamageSource.playerAttack((EntityHuman) damager);
        } else if (damager instanceof EntityArrow) {
            EntityArrow entityArrow = (EntityArrow) damager;
            damageSource = DamageSource.arrow(entityArrow, entityArrow.shooter);
        } else if (damager instanceof EntityFireball) {
            EntityFireball entityFireball = (EntityFireball) damager;
            damageSource = DamageSource.fireball(entityFireball, entityFireball.shooter);
        }

        if (damageSource == null) {
            if (damager instanceof EntityLiving) {
                damageSource = DamageSource.mobAttack((EntityLiving) damager);
            } else {
                return;
            }
        }

        damager.a(damaged, 0, damageSource);
    }

    /*
     *   World methods
     */

    @Override
    public void playPickupAnimation(LivingEntity livingEntity, Item item) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        EntityItem entityItem = (EntityItem) ((CraftItem) item).getHandle();
        EntityTracker entityTracker = ((WorldServer) entityLiving.world).getTracker();
        entityTracker.a(entityItem, new PacketPlayOutCollect(entityItem.getId(), entityLiving.getId(), item.getItemStack().getAmount()));
        //Makes sure the entity is still there.
        entityTracker.a(entityItem, new PacketPlayOutSpawnEntity(entityItem, 2, 1));
        entityTracker.a(entityItem, new PacketPlayOutEntityMetadata(entityItem.getId(), entityItem.getDataWatcher(), true));
    }

    @Override
    public void playDeathSound(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();

        SoundEffect deathSound = GET_SOUND_DEATH.invoke(entityLiving);
        float soundVolume = GET_SOUND_VOLUME.invoke(entityLiving);
        float soundPitch = GET_SOUND_PITCH.invoke(entityLiving);

        if (deathSound != null)
            entityLiving.a(deathSound, soundVolume, soundPitch);
    }

    @Override
    public void playParticle(String particle, Location location, int count, int offsetX, int offsetY, int offsetZ, double extra) {
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        world.sendParticles(null, EnumParticle.valueOf(particle), true, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                count, offsetX, offsetY, offsetZ, extra);
    }

    @Override
    public void playSpawnEffect(LivingEntity livingEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        if (entityLiving instanceof EntityInsentient)
            ((EntityInsentient) entityLiving).doSpawnEffect();
    }

    @Override
    public void startEntityListen(org.bukkit.World world) {
        ((CraftWorld) world).getHandle().addIWorldAccess(new IWorldAccess() {
            @Override
            public void a(World world, BlockPosition blockPosition, IBlockData iBlockData, IBlockData iBlockData1, int i) {

            }

            @Override
            public void a(BlockPosition blockPosition) {

            }

            @Override
            public void a(int i, int i1, int i2, int i3, int i4, int i5) {

            }

            @Override
            public void a(@Nullable EntityHuman entityHuman, SoundEffect soundEffect, SoundCategory soundCategory, double v, double v1, double v2, float v3, float v4) {

            }

            @Override
            public void a(SoundEffect soundEffect, BlockPosition blockPosition) {

            }

            @Override
            public void a(int i, boolean b, double v, double v1, double v2, double v3, double v4, double v5, int... ints) {

            }

            @Override
            public void a(int i, boolean b, boolean b1, double v, double v1, double v2, double v3, double v4, double v5, int... ints) {

            }

            @Override
            public void a(Entity entity) {

            }

            @Override
            public void b(Entity entity) {
                EntitiesListener.IMP.handleEntityRemove(entity.getBukkitEntity());
            }

            @Override
            public void a(int i, BlockPosition blockPosition, int i1) {

            }

            @Override
            public void a(EntityHuman entityHuman, int i, BlockPosition blockPosition, int i1) {

            }

            @Override
            public void b(int i, BlockPosition blockPosition, int i1) {

            }
        });
    }

    @Override
    public boolean handleEquipmentPickup(LivingEntity livingEntity, Item bukkitItem) {
        EntityLiving entityLiving = ((CraftLivingEntity) livingEntity).getHandle();

        if (!(entityLiving instanceof EntityInsentient))
            return false;

        EntityInsentient entityInsentient = (EntityInsentient) entityLiving;
        EntityItem entityItem = (EntityItem) ((CraftItem) bukkitItem).getHandle();
        ItemStack itemStack = entityItem.getItemStack().cloneItemStack();
        itemStack.setCount(1);

        EnumItemSlot equipmentSlotForItem = EntityInsentient.d(itemStack);

        if (equipmentSlotForItem.a() != EnumItemSlot.Function.ARMOR) {
            return false;
        }

        ItemStack equipmentItem = entityInsentient.getEquipment(equipmentSlotForItem);

        double equipmentDropChance = entityInsentient.dropChanceArmor[equipmentSlotForItem.b()];

        Random random = new Random();
        if (!equipmentItem.isEmpty() && Math.max(random.nextFloat() - 0.1F, 0.0F) < equipmentDropChance) {
            ENTITY_FORCE_DROPS.set(entityInsentient, true);
            entityInsentient.a(equipmentItem, 0F);
            ENTITY_FORCE_DROPS.set(entityInsentient, false);
        }

        entityInsentient.setSlot(equipmentSlotForItem, itemStack);
        entityInsentient.dropChanceArmor[equipmentSlotForItem.b()] = 2.0F;

        entityInsentient.persistent = true;
        entityInsentient.receive(entityItem, itemStack.getCount());

        return true;
    }

    @Override
    public void giveExp(Player player, int amount) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        ItemStack mendingItem = EnchantmentManager.b(Enchantments.C, entityPlayer);

        if (!mendingItem.isEmpty() && mendingItem.getItem().usesDurability()) {
            int repairAmount = Math.min(amount * 2, mendingItem.getData());
            amount -= repairAmount / 2;
            mendingItem.setData(mendingItem.getData() - repairAmount);
        }

        if (amount > 0) {
            PlayerExpChangeEvent playerExpChangeEvent = new PlayerExpChangeEvent(player, amount);
            Bukkit.getPluginManager().callEvent(playerExpChangeEvent);
            if (playerExpChangeEvent.getAmount() > 0)
                player.giveExp(playerExpChangeEvent.getAmount());
        }
    }

    @Override
    public void enterVehicle(Vehicle vehicle, org.bukkit.entity.Entity entity) {
        vehicle.addPassenger(entity);
    }

    @Override
    public int getPassengersCount(Vehicle vehicle) {
        return (int) ((CraftVehicle) vehicle).getHandle().passengers.stream()
                .filter(entity -> !(entity instanceof EntityPlayer)).count();
    }

    /*
     *   Tag methods
     */

    @Override
    public void updateEntity(org.bukkit.entity.LivingEntity sourceBukkit, org.bukkit.entity.LivingEntity targetBukkit) {
        EntityLiving source = ((CraftLivingEntity) sourceBukkit).getHandle();
        EntityLiving target = ((CraftLivingEntity) targetBukkit).getHandle();

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        source.b(nbtTagCompound);

        nbtTagCompound.setFloat("Health", source.getMaxHealth());
        nbtTagCompound.remove("SaddleItem");
        nbtTagCompound.remove("Saddle");
        nbtTagCompound.remove("ArmorItem");
        nbtTagCompound.remove("ArmorItems");
        nbtTagCompound.remove("HandItems");
        nbtTagCompound.remove("Leash");
        nbtTagCompound.remove("Leashed");
        if (targetBukkit instanceof Zombie)
            ((Zombie) targetBukkit).setBaby(nbtTagCompound.hasKey("IsBaby") && nbtTagCompound.getBoolean("IsBaby"));

        target.a(nbtTagCompound);
    }

    @Override
    public String serialize(org.bukkit.inventory.ItemStack itemStack) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();

        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        nmsItem.save(tagCompound);

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
        } catch (Exception ex) {
            return null;
        }

        return new BigInteger(1, outputStream.toByteArray()).toString(32);
    }

    @Override
    public org.bukkit.inventory.ItemStack deserialize(String serialized) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(serialized, 32).toByteArray());

        try {
            NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream));

            ItemStack nmsItem = new ItemStack(nbtTagCompoundRoot);

            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (Exception ex) {
            return null;
        }

    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public org.bukkit.inventory.ItemStack setTag(org.bukkit.inventory.ItemStack itemStack, String key, Object value) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();

        if (value instanceof Boolean)
            tagCompound.setBoolean(key, (boolean) value);
        else if (value instanceof Integer)
            tagCompound.setInt(key, (int) value);
        else if (value instanceof String)
            tagCompound.setString(key, (String) value);
        else if (value instanceof Double)
            tagCompound.setDouble(key, (double) value);
        else if (value instanceof Short)
            tagCompound.setShort(key, (short) value);
        else if (value instanceof Byte)
            tagCompound.setByte(key, (byte) value);
        else if (value instanceof Float)
            tagCompound.setFloat(key, (float) value);
        else if (value instanceof Long)
            tagCompound.setLong(key, (long) value);

        nmsItem.setTag(tagCompound);

        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public <T> T getTag(org.bukkit.inventory.ItemStack itemStack, String key, Class<T> valueType, Object def) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        if (nmsItem == null)
            return valueType.cast(def);

        NBTTagCompound tagCompound = nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound();

        if (tagCompound != null) {
            if (!tagCompound.hasKey(key))
                return valueType.cast(def);
            else if (valueType.equals(Boolean.class))
                return valueType.cast(tagCompound.getBoolean(key));
            else if (valueType.equals(Integer.class))
                return valueType.cast(tagCompound.getInt(key));
            else if (valueType.equals(String.class))
                return valueType.cast(tagCompound.getString(key));
            else if (valueType.equals(Double.class))
                return valueType.cast(tagCompound.getDouble(key));
            else if (valueType.equals(Short.class))
                return valueType.cast(tagCompound.getShort(key));
            else if (valueType.equals(Byte.class))
                return valueType.cast(tagCompound.getByte(key));
            else if (valueType.equals(Float.class))
                return valueType.cast(tagCompound.getFloat(key));
            else if (valueType.equals(Long.class))
                return valueType.cast(tagCompound.getLong(key));
        }

        throw new IllegalArgumentException("Cannot find nbt class type: " + valueType);
    }

    /*
     *   Other methods
     */

    @Override
    public void runAtEndOfTick(Runnable code) {
        ((CraftServer) Bukkit.getServer()).getServer().a(code::run);
    }

    /*
     *   Data methods
     */

    @Override
    public void saveEntity(StackedEntity stackedEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) stackedEntity.getLivingEntity()).getHandle();

        // Removing old tags
        entityLiving.getScoreboardTags().removeIf(tag -> tag.startsWith("ws:"));

        entityLiving.addScoreboardTag("ws:stack-amount=" + stackedEntity.getStackAmount());
        entityLiving.addScoreboardTag("ws:stack-cause=" + stackedEntity.getSpawnCause().name());
        if (stackedEntity.hasNameTag())
            entityLiving.addScoreboardTag("ws:name-tag=true");
        int upgradeId = ((WStackedEntity) stackedEntity).getUpgradeId();
        if (upgradeId != 0)
            entityLiving.addScoreboardTag("ws:upgrade=" + upgradeId);
    }

    @Override
    public void loadEntity(StackedEntity stackedEntity) {
        EntityLiving entityLiving = ((CraftLivingEntity) stackedEntity.getLivingEntity()).getHandle();
        for (String scoreboardTag : entityLiving.getScoreboardTags()) {
            if (scoreboardTag.startsWith("ws:")) {
                String[] tagSections = scoreboardTag.split("=");
                if (tagSections.length == 2) {
                    try {
                        String key = tagSections[0], value = tagSections[1];
                        if (key.equals("ws:stack-amount")) {
                            stackedEntity.setStackAmount(Integer.parseInt(value), false);
                        } else if (key.equals("ws:stack-cause")) {
                            stackedEntity.setSpawnCause(SpawnCause.valueOf(value));
                        } else if (key.equals("ws:name-tag")) {
                            ((WStackedEntity) stackedEntity).setNameTag();
                        } else if (key.equals("ws:upgrade")) {
                            ((WStackedEntity) stackedEntity).setUpgradeId(Integer.parseInt(value));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void saveItem(StackedItem stackedItem) {
        EntityItem entityItem = (EntityItem) ((CraftItem) stackedItem.getItem()).getHandle();

        // Removing old tags
        entityItem.getScoreboardTags().removeIf(tag -> tag.startsWith("ws:"));

        entityItem.addScoreboardTag("ws:stack-amount=" + stackedItem.getStackAmount());
    }

    @Override
    public void loadItem(StackedItem stackedItem) {
        EntityItem entityItem = (EntityItem) ((CraftItem) stackedItem.getItem()).getHandle();
        for (String scoreboardTag : entityItem.getScoreboardTags()) {
            if (scoreboardTag.startsWith("ws:")) {
                String[] tagSections = scoreboardTag.split("=");
                if (tagSections.length == 2) {
                    try {
                        String key = tagSections[0], value = tagSections[1];
                        if (key.equals("ws:stack-amount")) {
                            stackedItem.setStackAmount(Integer.parseInt(value), false);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static class SyncedCreatureSpawnerImpl extends CraftBlockState implements SyncedCreatureSpawner {

        private final World world;
        private final BlockPosition blockPosition;

        SyncedCreatureSpawnerImpl(Block block) {
            super(block);
            world = ((CraftWorld) block.getWorld()).getHandle();
            blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        }

        @Override
        public EntityType getSpawnedType() {
            try {
                MinecraftKey key = getSpawner().getSpawner().getMobName();
                return key == null ? EntityType.PIG : EntityType.fromName(key.getKey());
            } catch (Exception ex) {
                return EntityType.PIG;
            }
        }

        @Override
        public void setSpawnedType(EntityType entityType) {
            if (entityType != null && entityType.getName() != null) {
                getSpawner().getSpawner().setMobName(new MinecraftKey(entityType.getName()));
            } else {
                throw new IllegalArgumentException("Can't spawn EntityType " + entityType + " from mobspawners!");
            }
        }

        @Override
        public void setCreatureTypeByName(String s) {
            EntityType entityType = EntityType.fromName(s);
            if (entityType != null && entityType != EntityType.UNKNOWN)
                setSpawnedType(entityType);
        }

        @Override
        public String getCreatureTypeName() {
            MinecraftKey key = getSpawner().getSpawner().getMobName();
            return key == null ? "PIG" : key.getKey();
        }

        @Override
        public int getDelay() {
            return getSpawner().getSpawner().spawnDelay;
        }

        @Override
        public void setDelay(int i) {
            getSpawner().getSpawner().spawnDelay = i;
        }

        @Override
        public void updateSpawner(SpawnerUpgrade spawnerUpgrade) {
            MobSpawnerAbstract mobSpawnerAbstract = getSpawner().getSpawner();
            if (mobSpawnerAbstract instanceof NMSSpawners.StackedMobSpawner) {
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).minSpawnDelay = spawnerUpgrade.getMinSpawnDelay();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).maxSpawnDelay = spawnerUpgrade.getMaxSpawnDelay();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).spawnCount = spawnerUpgrade.getSpawnCount();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).maxNearbyEntities = spawnerUpgrade.getMaxNearbyEntities();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).requiredPlayerRange = spawnerUpgrade.getRequiredPlayerRange();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).spawnRange = spawnerUpgrade.getSpawnRange();
                ((NMSSpawners.StackedMobSpawner) mobSpawnerAbstract).updateUpgrade(spawnerUpgrade.getId());
            } else {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                mobSpawnerAbstract.b(nbtTagCompound);

                nbtTagCompound.setShort("MinSpawnDelay", (short) spawnerUpgrade.getMinSpawnDelay());
                nbtTagCompound.setShort("MaxSpawnDelay", (short) spawnerUpgrade.getMaxSpawnDelay());
                nbtTagCompound.setShort("SpawnCount", (short) spawnerUpgrade.getSpawnCount());
                nbtTagCompound.setShort("MaxNearbyEntities", (short) spawnerUpgrade.getMaxNearbyEntities());
                nbtTagCompound.setShort("RequiredPlayerRange", (short) spawnerUpgrade.getRequiredPlayerRange());
                nbtTagCompound.setShort("SpawnRange", (short) spawnerUpgrade.getSpawnRange());

                mobSpawnerAbstract.a(nbtTagCompound);
            }
        }

        @Override
        public SpawnerCachedData readData() {
            MobSpawnerAbstract mobSpawnerAbstract = getSpawner().getSpawner();
            if (mobSpawnerAbstract instanceof NMSSpawners.StackedMobSpawner) {
                NMSSpawners.StackedMobSpawner stackedMobSpawner = (NMSSpawners.StackedMobSpawner) mobSpawnerAbstract;
                return new SpawnerCachedData(
                        stackedMobSpawner.minSpawnDelay,
                        stackedMobSpawner.maxSpawnDelay,
                        stackedMobSpawner.spawnCount,
                        stackedMobSpawner.maxNearbyEntities,
                        stackedMobSpawner.requiredPlayerRange,
                        stackedMobSpawner.spawnRange,
                        stackedMobSpawner.spawnDelay / 20,
                        stackedMobSpawner.failureReason
                );
            } else {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                mobSpawnerAbstract.b(nbtTagCompound);
                return new SpawnerCachedData(
                        nbtTagCompound.getShort("MinSpawnDelay"),
                        nbtTagCompound.getShort("MaxSpawnDelay"),
                        nbtTagCompound.getShort("SpawnCount"),
                        nbtTagCompound.getShort("MaxNearbyEntities"),
                        nbtTagCompound.getShort("RequiredPlayerRange"),
                        nbtTagCompound.getShort("SpawnRange"),
                        nbtTagCompound.getShort("Delay") / 20
                );
            }
        }

        TileEntityMobSpawner getSpawner() {
            return (TileEntityMobSpawner) world.getTileEntity(blockPosition);
        }

    }

}