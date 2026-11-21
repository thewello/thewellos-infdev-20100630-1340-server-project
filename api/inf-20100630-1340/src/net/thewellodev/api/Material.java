package net.thewellodev.api;

import java.util.HashMap;
import java.util.Map;

/** Blocks and items of Infdev inf-20100630-1340 (the ids match the game). */
public enum Material {
    AIR(0), STONE(1), GRASS(2), DIRT(3), COBBLESTONE(4), WOOD(5), SAPLING(6), BEDROCK(7),
    WATER(8), STATIONARY_WATER(9), LAVA(10), STATIONARY_LAVA(11), SAND(12), GRAVEL(13),
    GOLD_ORE(14), IRON_ORE(15), COAL_ORE(16), LOG(17), LEAVES(18), SPONGE(19), GLASS(20),
    WOOL(35), YELLOW_FLOWER(37), RED_ROSE(38), BROWN_MUSHROOM(39), RED_MUSHROOM(40),
    GOLD_BLOCK(41), IRON_BLOCK(42), DOUBLE_STEP(43), STEP(44), BRICK(45), TNT(46),
    BOOKSHELF(47), MOSSY_COBBLESTONE(48), OBSIDIAN(49), TORCH(50), FIRE(51), MOB_SPAWNER(52),
    WOOD_STAIRS(53), CHEST(54), REDSTONE_WIRE(55), DIAMOND_ORE(56), DIAMOND_BLOCK(57),
    WORKBENCH(58), CROPS(59), SOIL(60), FURNACE(61), BURNING_FURNACE(62), SIGN_POST(63),
    WOODEN_DOOR(64), LADDER(65), RAILS(66), COBBLESTONE_STAIRS(67),
    // Items (256+)
    IRON_SPADE(256), IRON_PICKAXE(257), IRON_AXE(258), FLINT_AND_STEEL(259), APPLE(260), BOW(261),
    ARROW(262), COAL(263), DIAMOND(264), IRON_INGOT(265), GOLD_INGOT(266), IRON_SWORD(267),
    WOOD_SWORD(268), WOOD_SPADE(269), WOOD_PICKAXE(270), WOOD_AXE(271), STONE_SWORD(272),
    STONE_SPADE(273), STONE_PICKAXE(274), STONE_AXE(275), DIAMOND_SWORD(276), DIAMOND_SPADE(277),
    DIAMOND_PICKAXE(278), DIAMOND_AXE(279), STICK(280), BOWL(281), MUSHROOM_SOUP(282), GOLD_SWORD(283),
    GOLD_SPADE(284), GOLD_PICKAXE(285), GOLD_AXE(286), STRING(287), FEATHER(288), SULPHUR(289),
    WOOD_HOE(290), STONE_HOE(291), IRON_HOE(292), DIAMOND_HOE(293), GOLD_HOE(294), SEEDS(295), WHEAT(296),
    BREAD(297), LEATHER_HELMET(298), LEATHER_CHESTPLATE(299), LEATHER_LEGGINGS(300), LEATHER_BOOTS(301),
    CHAINMAIL_HELMET(302), CHAINMAIL_CHESTPLATE(303), CHAINMAIL_LEGGINGS(304), CHAINMAIL_BOOTS(305),
    IRON_HELMET(306), IRON_CHESTPLATE(307), IRON_LEGGINGS(308), IRON_BOOTS(309), DIAMOND_HELMET(310),
    DIAMOND_CHESTPLATE(311), DIAMOND_LEGGINGS(312), DIAMOND_BOOTS(313), GOLD_HELMET(314),
    GOLD_CHESTPLATE(315), GOLD_LEGGINGS(316), GOLD_BOOTS(317), FLINT(318), PORK(319), GRILLED_PORK(320),
    PAINTING(321), GOLDEN_APPLE(322), SIGN(323), WOOD_DOOR(324), BUCKET(325), WATER_BUCKET(326),
    LAVA_BUCKET(327), MINECART(328), SADDLE(329);

    private static final Map<Integer, Material> BY_ID = new HashMap<Integer, Material>();

    static {
        for (Material m : values()) {
            BY_ID.put(m.id, m);
        }
    }

    private final int id;

    Material(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Whether this id is a block that can be placed in the world (id &lt; 256). */
    public boolean isBlock() {
        return id < 256;
    }

    /** The material for an id, or null. */
    public static Material getMaterial(int id) {
        return BY_ID.get(id);
    }

    /** Finds a material by name (STONE, stone, "gold ore") or numeric id ("1"). */
    public static Material matchMaterial(String name) {
        if (name == null) {
            return null;
        }
        try {
            return getMaterial(Integer.parseInt(name.trim()));
        } catch (NumberFormatException ignored) {
        }
        String n = name.trim().toUpperCase(java.util.Locale.ENGLISH).replace(' ', '_').replace('-', '_');
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
