package com.xiaoyiluck.meowtools;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.invoke.MethodType;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks every external API member referenced by the actual baseline class files. */
class BinaryCompatibilityTest {
    @Test void allReferencedPaperAdventureAndPlaceholderMembersStillExist() throws Exception {
        Path classes = Path.of(MeowTools.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> failures = new ArrayList<>();
        try (var paths = Files.walk(classes.resolve("com/xiaoyiluck/meowtools"))) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".class")).toList()) inspect(file, failures);
        }
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    private void inspect(Path file, List<String> failures) throws Exception {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(file))) {
            assertEquals(0xCAFEBABE, input.readInt()); input.readUnsignedShort();
            assertEquals(65, input.readUnsignedShort(), "Universal plugin must remain Java 21 bytecode");
            int count = input.readUnsignedShort();
            Object[] values = new Object[count]; int[] tags = new int[count];
            for (int i = 1; i < count; i++) {
                int tag = tags[i] = input.readUnsignedByte();
                switch (tag) {
                    case 1 -> values[i] = input.readUTF();
                    case 3, 4 -> input.readInt();
                    case 5, 6 -> { input.readLong(); i++; }
                    case 7, 8, 16, 19, 20 -> values[i] = input.readUnsignedShort();
                    case 9, 10, 11, 12, 17, 18 -> values[i] = new int[] {input.readUnsignedShort(), input.readUnsignedShort()};
                    case 15 -> { input.readUnsignedByte(); input.readUnsignedShort(); }
                    default -> throw new IOException("Unknown constant pool tag " + tag);
                }
            }
            ClassLoader loader = getClass().getClassLoader();
            for (int i = 1; i < count; i++) {
                if (tags[i] < 9 || tags[i] > 11) continue;
                int[] reference = (int[]) values[i];
                String owner = ((String) values[(int) values[reference[0]]]).replace('/', '.');
                if (!owner.startsWith("org.bukkit.") && !owner.startsWith("com.destroystokyo.")
                        && !owner.startsWith("io.papermc.") && !owner.startsWith("net.kyori.") && !owner.startsWith("me.clip.")) continue;
                int[] nameType = (int[]) values[reference[1]];
                String name = (String) values[nameType[0]], descriptor = (String) values[nameType[1]];
                try {
                    Class<?> type = Class.forName(owner, false, loader);
                    if (tags[i] == 9) {
                        var field = type.getField(name);
                        if (!field.getType().descriptorString().equals(descriptor)) throw new NoSuchFieldException(descriptor);
                    } else {
                        if ((tags[i] == 11) != type.isInterface()) throw new IllegalStateException("Class/interface changed");
                        MethodType signature = MethodType.fromMethodDescriptorString(descriptor, loader);
                        if (name.equals("<init>")) type.getDeclaredConstructor(signature.parameterArray());
                        else if (java.util.stream.Stream.concat(Arrays.stream(type.getMethods()), Arrays.stream(Object.class.getMethods())).noneMatch(method -> method.getName().equals(name)
                                && MethodType.methodType(method.getReturnType(), method.getParameterTypes()).equals(signature))) {
                            throw new NoSuchMethodException(descriptor);
                        }
                    }
                } catch (ReflectiveOperationException | LinkageError | IllegalStateException failure) {
                    failures.add(file.getFileName() + ": " + owner + "." + name + descriptor + " -> " + failure);
                }
            }
        }
    }
}
