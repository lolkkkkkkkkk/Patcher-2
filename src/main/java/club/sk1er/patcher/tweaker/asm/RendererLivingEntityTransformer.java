package club.sk1er.patcher.tweaker.asm;

import club.sk1er.patcher.tweaker.transform.PatcherTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class RendererLivingEntityTransformer implements PatcherTransformer {
    @Override
    public String[] getClassName() {
        return new String[]{"net.minecraft.client.renderer.entity.RendererLivingEntity"};
    }

    @Override
    public void transform(ClassNode classNode, String name) {
        for (MethodNode method : classNode.methods) {
            String methodName = mapMethodName(classNode, method);
            if (methodName.equalsIgnoreCase("doRender")) {//TODO mappings stuff
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                /*
                Find             if (shouldSit && entity.ridingEntity instanceof EntityLivingBase)
                    go back to find vars # of f2, f1, f

                    go forward until we find the label we jump to if that statement if false, retract 1 insn, then read f2 = f1 -f
                 */
                int f = 0;
                int f1 = 0;
                int f2 = 0;

                int i = 0;
                while (iterator.hasNext()) {
                    AbstractInsnNode next = iterator.next();
                    if (next instanceof TypeInsnNode) {
                        if (next.getOpcode() == Opcodes.INSTANCEOF && ((TypeInsnNode) next).desc.equalsIgnoreCase("net/minecraft/entity/EntityLivingBase")) {

                            //Find values of f2,f1,f
                            while ((next = next.getPrevious()) != null) {
                                if (next instanceof VarInsnNode && next.getOpcode() == Opcodes.FSTORE) {
                                    if (i == 0) {
                                        f2 = ((VarInsnNode) next).var;
                                    } else if (i == 1) {
                                        f1 = ((VarInsnNode) next).var;
                                    } else {
                                        f = ((VarInsnNode) next).var;
                                    }
                                    i++;
                                    if (i == 3) break;
                                }
                            }
                            if (next == null) return;

                            LabelNode node = null; //Find label
                            while ((next = next.getNext()) != null) {
                                if (next instanceof JumpInsnNode && next.getOpcode() == Opcodes.IFEQ) {
                                    node = ((JumpInsnNode) next).label;
                                    break;
                                }
                            }
                            if (next == null) return;

                            LabelNode labelNode = new LabelNode(); //Override final if statement to jump to our new end of block
                            while ((next = next.getNext()) != null) {
                                if (next instanceof JumpInsnNode && ((JumpInsnNode) next).label.equals(node) && next.getOpcode() == Opcodes.IFLE) {
                                    ((JumpInsnNode) next).label = labelNode;
                                    break;
                                }
                            }
                            if (next == null) return;

                            while ((next = next.getNext()) != null) {
                                if (next.equals(node)) {
                                    InsnList insnList = new InsnList();
                                    insnList.add(labelNode);
                                    insnList.add(new VarInsnNode(Opcodes.FLOAD, f1));
                                    insnList.add(new VarInsnNode(Opcodes.FLOAD, f));
                                    insnList.add(new InsnNode(Opcodes.FSUB));
                                    insnList.add(new VarInsnNode(Opcodes.FSTORE, f2));
                                    method.instructions.insertBefore(next, insnList);
                                    return;
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}
