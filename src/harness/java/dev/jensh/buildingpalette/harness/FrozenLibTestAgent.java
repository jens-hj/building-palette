package dev.jensh.buildingpalette.harness;

import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.instrument.*;
import java.security.ProtectionDomain;

/** Test-only workaround for FrozenLib 3.0.3 loading Minecraft translations from a mixin
 * plugin's development log. Normal launches don't execute that log. No jar on disk changes. */
public final class FrozenLibTestAgent {
    public static void premain(String ignored, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type, ProtectionDomain domain, byte[] bytes) {
                if (!name.equals("net/frozenblock/lib/FrozenLibEarlyConstants")) return null;
                ClassFile cf = ClassFile.of();
                return cf.transformClass(cf.parse(bytes), (builder, element) -> {
                    if (element instanceof MethodModel method && method.methodName().stringValue().equals("isDatagen")) {
                        builder.transformMethod(method, (methodBuilder, methodElement) -> {
                            if (methodElement instanceof CodeModel code) {
                                methodBuilder.transformCode(code, (codeBuilder, instruction) -> {
                                    if (instruction instanceof InvokeInstruction call
                                            && call.owner().asInternalName().equals("net/frozenblock/lib/platform/ModLoader")
                                            && call.name().stringValue().equals("isDevelopmentEnvironment")) {
                                        codeBuilder.iconst_0();
                                    } else codeBuilder.with(instruction);
                                });
                            } else methodBuilder.with(methodElement);
                        });
                    } else builder.with(element);
                });
            }
        });
    }
}
