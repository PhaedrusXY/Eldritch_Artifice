package com.eldritchartifice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Tier-5 Eldritch armor item registration.
 *
 * <p>The standalone balance target is 18 total armor and 2 total toughness. The worn texture uses
 * the crimson Open Eye vestment identity while preserving stable save IDs.</p>
 */
final class EldritchArmorRegistry {
    static final ResourceLocation HELMET_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch_helmet");
    static final ResourceLocation CHESTPLATE_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch_chestplate");
    static final ResourceLocation LEGGINGS_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch_leggings");
    static final ResourceLocation BOOTS_ID =
            new ResourceLocation(EldritchArtifice.MOD_ID, "eldritch_boots");

    static final Item HELMET = createArmorItem("HELMET");
    static final Item CHESTPLATE = createArmorItem("CHESTPLATE");
    static final Item LEGGINGS = createArmorItem("LEGGINGS");
    static final Item BOOTS = createArmorItem("BOOTS");

    private EldritchArmorRegistry() {
    }

    static void onRegisterEvent(Object event) {
        if (!(event instanceof RegisterEvent registerEvent)) {
            return;
        }

        registerEvent.register(
                ForgeRegistries.ITEMS.getRegistryKey(),
                helper -> {
                    helper.register(HELMET_ID, HELMET);
                    helper.register(CHESTPLATE_ID, CHESTPLATE);
                    helper.register(LEGGINGS_ID, LEGGINGS);
                    helper.register(BOOTS_ID, BOOTS);
                    RuntimeLog.info(
                            "Registered Tier-5 Eldritch armor items: "
                                    + HELMET_ID + ", " + CHESTPLATE_ID + ", "
                                    + LEGGINGS_ID + ", " + BOOTS_ID + ".");
                });
    }

    static boolean hasFullSet(Object entity) {
        return slotMatches(entity, "HEAD", HELMET)
                && slotMatches(entity, "CHEST", CHESTPLATE)
                && slotMatches(entity, "LEGS", LEGGINGS)
                && slotMatches(entity, "FEET", BOOTS);
    }

    static String setStatus(Object entity) {
        return "head=" + slotMatches(entity, "HEAD", HELMET)
                + ", chest=" + slotMatches(entity, "CHEST", CHESTPLATE)
                + ", legs=" + slotMatches(entity, "LEGS", LEGGINGS)
                + ", feet=" + slotMatches(entity, "FEET", BOOTS)
                + ", fullSet=" + hasFullSet(entity);
    }

    private static boolean slotMatches(Object entity, String slotName, Item expected) {
        if (entity == null || expected == null) {
            return false;
        }

        try {
            Class<?> slotClass = Class.forName("net.minecraft.world.entity.EquipmentSlot");
            Object slot = enumConstant(slotClass, slotName);
            Method getItemBySlot = findOneArgMethod(
                    entity.getClass(),
                    new String[]{"getItemBySlot", "m_6844_"},
                    slotClass);
            if (getItemBySlot == null) {
                return false;
            }

            Object stack = getItemBySlot.invoke(entity, slot);
            if (stack == null) {
                return false;
            }

            Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
            Method getItem = findNoArgMethod(
                    stack.getClass(),
                    new String[]{"getItem", "m_41720_"},
                    itemClass);
            return getItem != null && getItem.invoke(stack) == expected;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            RuntimeLog.warn("Could not inspect Eldritch armor slot "
                    + slotName.toLowerCase(Locale.ROOT) + ": " + exception);
            return false;
        }
    }

    private static Item createArmorItem(String typeName) {
        try {
            Class<?> materialInterface =
                    Class.forName("net.minecraft.world.item.ArmorMaterial");
            Class<?> materialsClass =
                    Class.forName("net.minecraft.world.item.ArmorMaterials");
            Class<?> armorItemClass =
                    Class.forName("net.minecraft.world.item.ArmorItem");
            Class<?> armorTypeClass =
                    Class.forName("net.minecraft.world.item.ArmorItem$Type");
            Class<?> propertiesClass =
                    Class.forName("net.minecraft.world.item.Item$Properties");

            Object material = EldritchArmorMaterial.INSTANCE;
            Object armorType = enumConstant(armorTypeClass, typeName);
            Object properties = propertiesClass.getConstructor().newInstance();

            Constructor<?> constructor = armorItemClass.getConstructor(
                    materialInterface,
                    armorTypeClass,
                    propertiesClass);
            return (Item) constructor.newInstance(material, armorType, properties);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to construct Eldritch armor shell for " + typeName + ".",
                    exception);
        }
    }

    private static Object enumConstant(Class<?> enumClass, String name) {
        for (Object value : enumClass.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) {
                return value;
            }
        }
        throw new IllegalStateException(
                "Enum constant " + name + " was not found in " + enumClass.getName());
    }

    private static Method findNoArgMethod(
            Class<?> owner,
            String[] names,
            Class<?> returnType) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name);
                if (returnType.isAssignableFrom(method.getReturnType())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findOneArgMethod(
            Class<?> owner,
            String[] names,
            Class<?> parameterType) {
        for (String name : names) {
            try {
                return owner.getMethod(name, parameterType);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
