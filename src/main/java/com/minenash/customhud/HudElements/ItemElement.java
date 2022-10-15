package com.minenash.customhud.HudElements;

import com.minenash.customhud.Flags;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.function.Function;

public class ItemElement implements HudElement {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static ItemStack stack(int slot) {return client.player.getStackReference(slot).get();}
    private static Item item(int slot) {return client.player.getStackReference(slot).get().getItem();}

    public static final Function<Integer, String> ID = (slot) -> Registry.ITEM.getId(item(slot)).toString();
    public static final Function<Integer, String> NAME = (slot) -> item(slot).getName().getString();
    public static final Function<Integer, Number> RAW_ID = (slot) -> Item.getRawId(item(slot));
    public static final Function<Integer, Boolean> IS_STACK_EMPTY = (slot) -> stack(slot).isEmpty();

    public static final Function<Integer, String> CUSTOM_NAME = (slot) -> stack(slot).getName().getString();
    public static final Function<Integer, Number> CUSTOM_NAME_LENGTH = (slot) -> stack(slot).getName().getString().length();
    public static final Function<Integer, Boolean> HAS_CUSTOM_NAME = (slot) -> !stack(slot).getName().getString().equals(item(slot).getName().getString());

    public static final Function<Integer, Number> DURABILITY_NUM = (slot) -> stack(slot).getMaxDamage() - stack(slot).getDamage();
    public static final Function<Integer, Number> MAX_DURABILITY_NUM = (slot) -> stack(slot).getMaxDamage();
    public static final Function<Integer, String> DURABILITY_STR = (slot) -> Integer.toString( stack(slot).getMaxDamage() - stack(slot).getDamage() );
    public static final Function<Integer, String> MAX_DURABILITY_STR = (slot) -> Integer.toString( item(slot).getMaxDamage() );
    public static final Function<Integer, Number> DURABILITY_PERCENT_NUM = (slot) -> 100 - stack(slot).getDamage() / (float) stack(slot).getMaxDamage() * 100;
    public static final Function<Integer, String> DURABILITY_PERCENT_STR = (slotAndPrecision) -> {
        int slot = slotAndPrecision & 0x000000FF;
        int precision = slotAndPrecision & 0xFFFFFF00;
        int maxDamage = stack(slot).getMaxDamage();
        if (maxDamage == 0)
            return precision == 0 ? "0" : "0.0";
        if (precision == 0)
            return Integer.toString( 100 - (int)(stack(slot).getDamage() / (float) maxDamage * 100) );
        float exponent = (float) Math.pow(10, precision);
        return Float.toString( 100 - (int)(stack(slot).getDamage() / (float) maxDamage * 100 * exponent) / exponent );
    };

    public static final Function<Integer, Boolean> HAS_DURABILITY = (slot) ->  item(slot).getMaxDamage() - client.player.getMainHandStack().getDamage() > 0;
    public static final Function<Integer, Boolean> HAS_MAX_DURABILITY = (slot) ->  item(slot).getMaxDamage() > 0;

    public static Pair<HudElement,String> create(String slotString, String method, Flags flags) {
        int slot = getSlotNumber(slotString);
        if (slot == -1)
            return new Pair<>(null, "Unknown slot: " + slotString);
        if (slot > 35 && slot < 98 || slot > 103)
            return new Pair<>(null, "That slot is not available to the player: " + slotString);

        ItemElement element = switch (method) {
            case "" -> new ItemElement(slot, NAME, RAW_ID, IS_STACK_EMPTY);
            case "id" -> new ItemElement(slot, ID, RAW_ID, IS_STACK_EMPTY);
            case "name" -> new ItemElement(slot, CUSTOM_NAME, CUSTOM_NAME_LENGTH, HAS_CUSTOM_NAME);
            case "dur","durability" -> new ItemElement(slot, DURABILITY_STR, DURABILITY_NUM, HAS_DURABILITY);
            case "max_dur","max_durability" -> new ItemElement(slot, MAX_DURABILITY_STR, MAX_DURABILITY_NUM, HAS_MAX_DURABILITY);
            case "dur_per","durability_percentage" -> new ItemElement(slot, DURABILITY_PERCENT_STR, DURABILITY_PERCENT_NUM, HAS_MAX_DURABILITY);
            default -> null;
        };

        if (element == null)
            return new Pair<>(null, "Unknown property: " + method);

        if (flags.anyUsed())
            return new Pair<>(new FormattedElement(element, flags), null);

        if (element.str == DURABILITY_PERCENT_STR)
            element.precision = flags.precision == -1? 0 : flags.precision << 8;

        return new Pair<>(element, null);

    }

    private static int getSlotNumber(String in) {
        try {
            return ItemSlotArgumentType.itemSlot().parse(new StringReader(switch (in) {
                case "head", "chest", "legs", "feet" -> "armor." + in;
                case "mainhand", "offhand" -> "weapon" + in;
                default -> {
                    if (in.charAt(0) == 'h' && in.charAt(1) != 'o') yield "hotbar." + in.substring(1);
                    if (in.charAt(0) == 'i' && in.charAt(1) != 'n') yield "inventory." + in.substring(1);
                    yield in;
                }
            }));
        } catch (CommandSyntaxException e) {
            return -1;
        }
    }


    private int precision = -1;
    private final int slot;
    private final Function<Integer, String> str;
    private final Function<Integer, Number> num;
    private final Function<Integer, Boolean> bool;
    public ItemElement(int slot, Function<Integer, String> str, Function<Integer, Number> num, Function<Integer, Boolean> bool) {
        this.slot = slot;
        this.str = str;
        this.num = num;
        this.bool = bool;
    }

    @Override
    public String getString() {
        if (precision == -1)
            return str.apply(slot);

        return str.apply(slot + precision);

    }

    @Override
    public Number getNumber() {
        return num.apply(slot);
    }

    @Override
    public boolean getBoolean() {
        return bool.apply(slot);
    }
}
