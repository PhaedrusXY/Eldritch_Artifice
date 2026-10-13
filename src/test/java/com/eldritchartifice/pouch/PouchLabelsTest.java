package com.eldritchartifice.pouch;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class PouchLabelsTest {
    private static CompoundTag ritual(String id) {
        CompoundTag entry = new CompoundTag();
        entry.putString(PouchLabels.RITUAL_KEY, id);
        entry.putInt("ritual_reagent_count", 2);
        CompoundTag reagent = new CompoundTag();
        reagent.putString("marker_destination", "Do not alter");
        entry.put("test_reagent", reagent);
        return entry;
    }

    private static CompoundTag pouch() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int slot = 0; slot < PouchLabels.SLOT_COUNT; slot++) list.add(ritual("mna:test_" + slot));
        root.put(PouchLabels.DATA_KEY, list);
        root.putInt("ritual_kit_index", 3);
        root.putInt("unrelated_patch_data", 17);
        return root;
    }

    @Test void labelChangesOnlyItsOwnNestedKey() {
        CompoundTag root = pouch();
        CompoundTag before = root.copy();
        PouchLabels.setLabel(PouchLabels.entry(root, 3), "Portal - Home");
        assertEquals("Portal - Home", PouchLabels.label(PouchLabels.entry(root, 3)));
        assertEquals("", PouchLabels.label(PouchLabels.entry(root, 2)));
        assertFalse(root.contains(PouchLabels.LABEL_KEY));
        PouchLabels.entry(root, 3).remove(PouchLabels.LABEL_KEY);
        assertEquals(before, root);
    }

    @Test void overwritingWithDifferentRitualDropsOnlyThatLabel() {
        CompoundTag root = pouch();
        PouchLabels.setLabel(PouchLabels.entry(root, 2), "Keep this");
        PouchLabels.setLabel(PouchLabels.entry(root, 3), "Discard this");
        root.getList(PouchLabels.DATA_KEY, 10).set(3, ritual("mna:different_ritual"));
        assertEquals("", PouchLabels.label(PouchLabels.entry(root, 3)));
        assertEquals("Keep this", PouchLabels.label(PouchLabels.entry(root, 2)));
    }

    @Test void overwritingWithIdenticalRitualAlsoDropsItsLabel() {
        CompoundTag root = pouch();
        CompoundTag originalConfiguration = PouchLabels.entry(root, 3).copy();
        PouchLabels.setLabel(PouchLabels.entry(root, 3), "Portal - Old destination");
        root.getList(PouchLabels.DATA_KEY, 10).set(3, originalConfiguration);
        assertEquals("", PouchLabels.label(PouchLabels.entry(root, 3)));
        assertFalse(PouchLabels.entry(root, 3).contains(PouchLabels.LABEL_KEY));
    }

    @Test void clearingSlotDropsLabelAndCannotCreateAnEmptySavedRitual() {
        CompoundTag root = pouch();
        PouchLabels.setLabel(PouchLabels.entry(root, 3), "Old");
        root.getList(PouchLabels.DATA_KEY, 10).set(3, new CompoundTag());
        PouchLabels.setLabel(PouchLabels.entry(root, 3), "Must not exist");
        assertEquals(new CompoundTag(), PouchLabels.entry(root, 3));
        assertFalse(PouchLabels.occupied(PouchLabels.entry(root, 3)));
    }

    @Test void resetAndWhitespaceRemoveKeyWithoutChangingConfiguration() {
        CompoundTag entry = ritual("mna:test");
        CompoundTag before = entry.copy();
        PouchLabels.setLabel(entry, "Temporary");
        PouchLabels.setLabel(entry, "  \t\n\u2003  ");
        assertEquals(before, entry);
        PouchLabels.setLabel(entry, "Temporary");
        PouchLabels.setLabel(entry, "");
        assertEquals(before, entry);
    }

    @Test void labelsSurviveNbtSaveLoadAndPouchCopiesAreIndependent() throws Exception {
        CompoundTag root = pouch();
        PouchLabels.setLabel(PouchLabels.entry(root, 7), "Home - Twilight Forest");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, output);
        CompoundTag loaded = NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(root, loaded);
        assertEquals("Home - Twilight Forest", PouchLabels.label(PouchLabels.entry(loaded, 7)));
        PouchLabels.setLabel(PouchLabels.entry(loaded, 7), "Changed on second pouch");
        assertEquals("Home - Twilight Forest", PouchLabels.label(PouchLabels.entry(root, 7)));
    }

    @Test void readsAreNonMutatingAndMalformedEntriesAreRejected() {
        CompoundTag empty = new CompoundTag();
        assertNull(PouchLabels.entry((CompoundTag) null, 0));
        assertNull(PouchLabels.entry(empty, 0));
        assertEquals(new CompoundTag(), empty);
        assertNull(PouchLabels.entry(pouch(), -1));
        assertNull(PouchLabels.entry(pouch(), 8));
        empty.putString(PouchLabels.DATA_KEY, "wrong_type");
        assertNull(PouchLabels.entry(empty, 0));
        ListTag wrongList = new ListTag();
        wrongList.add(StringTag.valueOf("wrong_entry_type"));
        empty.put(PouchLabels.DATA_KEY, wrongList);
        assertNull(PouchLabels.entry(empty, 0));
    }

    @Test void legacySingleCompoundMatchesNativePouchReading() {
        CompoundTag root = new CompoundTag();
        CompoundTag legacy = ritual("mna:legacy");
        root.put(PouchLabels.DATA_KEY, legacy);
        assertSame(legacy, PouchLabels.entry(root, 0));
        assertSame(legacy, PouchLabels.entry(root, 7));
        PouchLabels.setLabel(legacy, "Legacy name");
        assertEquals("Legacy name", PouchLabels.label(PouchLabels.entry(root, 0)));
    }

    @Test void namesAreBoundedLiteralUnicodeText() {
        assertEquals("", PouchLabels.sanitize(null));
        assertEquals("Portal Home", PouchLabels.sanitize("  Portal\u2003Home  "));
        assertEquals("Home", PouchLabels.sanitize("\u202eHo\nme\u0000\u00a7"));
        assertEquals("a".repeat(64), PouchLabels.sanitize("a".repeat(100)));
        assertEquals("a".repeat(63), PouchLabels.sanitize("a".repeat(63) + "\ud83d\udd2e"));
        assertEquals("\u9580 - Caf\u00e9 \ud83d\udd2e", PouchLabels.sanitize("\u9580 - Caf\u00e9 \ud83d\udd2e"));
        assertEquals("xy", PouchLabels.sanitize("x\ud800y"));
    }

    @Test void sessionAcceptsOnlyOriginalPouchAndUnchangedEntryBeforeExpiry() {
        Object stack = new Object();
        CompoundTag entry = ritual("mna:test");
        PouchEditSession<Object> session = new PouchEditSession<>(UUID.randomUUID(), 0, 3, stack, entry, 100);
        assertTrue(session.matches(stack, entry, 99));
        assertFalse(session.matches(new Object(), entry, 99));
        assertFalse(session.matches(stack, entry, 100));
        assertFalse(session.matches(stack, null, 99));
        entry.putInt("ritual_reagent_count", 9);
        assertFalse(session.matches(stack, entry, 99));
    }

    @Test void equalReplacementCannotReceiveDelayedRename() {
        Object stack = new Object();
        CompoundTag original = ritual("mna:test");
        PouchEditSession<Object> session = new PouchEditSession<>(UUID.randomUUID(), 0, 3, stack, original, 100);
        CompoundTag replacement = original.copy();
        assertEquals(original, replacement);
        assertFalse(session.matches(stack, replacement, 99));
    }

    @Test void labelChangeDuringEditingInvalidatesOldTicket() {
        Object stack = new Object();
        CompoundTag entry = ritual("mna:test");
        PouchEditSession<Object> session = new PouchEditSession<>(UUID.randomUUID(), 0, 3, stack, entry, 100);
        PouchLabels.setLabel(entry, "Changed elsewhere");
        assertFalse(session.matches(stack, entry, 99));
    }

    @Test void pinnedMnaActuallyReplacesSlotWithFreshCompound() throws Exception {
        // Check the real dependency bytecode, not just a simulated overwrite.
        // This intentionally fails if a future MnA version starts merging old entries.
        ClassNode node = readClass("com/mna/items/ritual/PractitionersPouch.class");
        MethodNode method = node.methods.stream().filter(m -> m.name.equals("storeRitual"))
                .findFirst().orElseThrow();
        AbstractInsnNode first = meaningful(method.instructions.getFirst());
        assertTrue(first instanceof TypeInsnNode);
        assertEquals(Opcodes.NEW, first.getOpcode());
        assertEquals("net/minecraft/nbt/CompoundTag", ((TypeInsnNode) first).desc);
        AbstractInsnNode store = first;
        while (store != null && store.getOpcode() != Opcodes.ASTORE) store = meaningful(store.getNext());
        assertNotNull(store);
        int freshLocal = ((VarInsnNode) store).var;
        MethodInsnNode replacement = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("net/minecraft/nbt/ListTag")
                    && call.desc.equals("(ILnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;")) replacement = call;
        }
        assertNotNull(replacement, "Expected ListTag.set(index, freshCompound)");
        AbstractInsnNode value = replacement.getPrevious();
        while (value != null && value.getOpcode() < 0) value = value.getPrevious();
        assertTrue(value instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, value.getOpcode());
        assertEquals(freshLocal, ((VarInsnNode) value).var);
        // No reads or merges on any CompoundTag within storeRitual: the old label cannot be copied.
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("net/minecraft/nbt/CompoundTag")) {
                assertFalse(call.desc.equals("(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;"));
                assertFalse(call.desc.equals("(Ljava/lang/String;)Ljava/lang/String;"));
            }
        }
        ClassNode mbag = readClass("com/mna/items/relic/MithionsMagnificentMbag.class");
        assertEquals("com/mna/items/ritual/PractitionersPouch", mbag.superName);
    }

    private static AbstractInsnNode meaningful(AbstractInsnNode instruction) {
        while (instruction != null && instruction.getOpcode() < 0) instruction = instruction.getNext();
        return instruction;
    }

    private static ClassNode readClass(String resource) throws Exception {
        try (InputStream stream = PouchLabelsTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
