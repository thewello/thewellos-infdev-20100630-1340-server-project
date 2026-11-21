package net.thewellodev.api;

/** A stack of items: type (item or block id), amount, and damage/data value. */
public final class ItemStack {

    private final int typeId;
    private final int amount;
    private final int data;

    public ItemStack(int typeId, int amount) {
        this(typeId, amount, 0);
    }

    public ItemStack(int typeId, int amount, int data) {
        this.typeId = typeId;
        this.amount = amount;
        this.data = data;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getAmount() {
        return amount;
    }

    public int getData() {
        return data;
    }

    /** The {@link Material} of this type, or null if unknown. */
    public Material getType() {
        return Material.getMaterial(typeId);
    }

    @Override
    public String toString() {
        Material m = getType();
        return (m == null ? String.valueOf(typeId) : m.name()) + " x" + amount;
    }
}
