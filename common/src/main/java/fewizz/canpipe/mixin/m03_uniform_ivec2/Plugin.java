package fewizz.canpipe.mixin.m03_uniform_ivec2;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class Plugin implements IMixinConfigPlugin, Opcodes {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.equals("fewizz.canpipe.mixin.m03_uniform_ivec2.UniformTypeMixin")) {
            patchUniformType(targetClass);
        }
    }

    private static void patchUniformType(ClassNode classNode) {
        String desc = "L"+classNode.name+";";
        MethodNode arrayInitMethod = classNode.methods.stream()
            .filter(m -> !m.name.equals("values") && m.desc.equals("()["+desc)).findFirst().get();

        MethodNode classInitMethod = classNode.methods.stream()
            .filter(m -> m.name.equals("<clinit>")).findFirst().get();

        TypeInsnNode aNewArrayInsn = (TypeInsnNode) StreamSupport.stream(arrayInitMethod.instructions.spliterator(), false)
            .filter(insn->insn.getOpcode() == ANEWARRAY).findFirst().get();

        IntInsnNode sizeArgInsn = (IntInsnNode) aNewArrayInsn.getPrevious();
        // if (sizeArgInsn.getOpcode() != BIPUSH) { throw new RuntimeException(); }
        //int i = 3;  // TODO actually compute size
        sizeArgInsn.operand += 1;

        classNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_FINAL | ACC_STATIC | ACC_ENUM, "IVEC2", desc, null, null));

        InsnList createNewEntry = new InsnList();
        createNewEntry.add(new TypeInsnNode(NEW, classNode.name));
        createNewEntry.add(new InsnNode(DUP));
        createNewEntry.add(new LdcInsnNode("IVEC2"));
        createNewEntry.add(new IntInsnNode(SIPUSH, sizeArgInsn.operand-1));
        createNewEntry.add(new InsnNode(ICONST_2));
        createNewEntry.add(new LdcInsnNode("ivec2"));
        createNewEntry.add(new MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "(Ljava/lang/String;IILjava/lang/String;)V"));
        createNewEntry.add(new FieldInsnNode(PUTSTATIC, classNode.name, "IVEC2", desc));
        classInitMethod.instructions.insertBefore(
            StreamSupport.stream(classInitMethod.instructions.spliterator(), false)
                .filter(insn -> insn.getOpcode() == NEW)
                .findFirst().get(),
            createNewEntry
        );

        InsnList addNewEntry = new InsnList();
        addNewEntry.add(new InsnNode(DUP)); // dup array
        addNewEntry.add(new IntInsnNode(BIPUSH, sizeArgInsn.operand-1));
        addNewEntry.add(new FieldInsnNode(GETSTATIC, classNode.name, "IVEC2", desc));
        addNewEntry.add(new InsnNode(AASTORE));
        arrayInitMethod.instructions.insert(aNewArrayInsn, addNewEntry);
    }

}
