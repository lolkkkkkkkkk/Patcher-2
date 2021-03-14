/*
 * Copyright © 2020 by Sk1er LLC
 *
 * All rights reserved.
 *
 * Sk1er LLC
 * 444 S Fulton Ave
 * Mount Vernon, NY
 * sk1er.club
 */
package club.sk1er.patcher.asm.forge

import club.sk1er.hookinjection.getInstructionsWithTryCatchNodes
import club.sk1er.patcher.hooks.ModelLoaderHook
import club.sk1er.patcher.tweaker.transform.PatcherTransformer
import codes.som.anthony.koffee.assembleBlock
import codes.som.anthony.koffee.insns.jvm.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class ModelLoaderTransformer : PatcherTransformer {
    /**
     * The class name that's being transformed
     *
     * @return the class name
     */
    override fun getClassName(): Array<String> {
        return arrayOf("net.minecraftforge.client.model.ModelLoader")
    }

    /**
     * Perform any asm in order to transform code
     *
     * @param classNode the transformed class node
     * @param name      the transformed class name
     */
    override fun transform(classNode: ClassNode, name: String) {
        classNode.version = Opcodes.V1_8
        classNode.interfaces.add(getHookClass("accessors/IModelLoader"))
        val callLoadBlocks = MethodNode(Opcodes.ACC_PUBLIC, "callLoadBlocks", "()V", null, null)
        callLoadBlocks.instructions.add(createCallLoadBlocks())
        classNode.methods.add(callLoadBlocks)
        val callLoadItems = MethodNode(Opcodes.ACC_PUBLIC, "callLoadItems", "()V", null, null)
        callLoadItems.instructions.add(createCallLoadItems())
        classNode.methods.add(callLoadItems)
        classNode.methods.forEach {
            when (mapMethodName(classNode, it)) {
                "onPostBakeEvent" -> {
                    for (insn in it.instructions) {
                        if (insn is FieldInsnNode && insn.name == "isLoading") {
                            it.instructions.insertBefore(insn.previous, clearMemory())
                            break
                        }
                    }
                }

                "setupModelRegistry", "func_177570_a" -> {
                    clearInstructions(it)
                    it.instructions.insert(getAsyncLoader(it))
                }
            }
        }
    }

    private fun getAsyncLoader(methodNode: MethodNode): InsnList {
        val list = InsnList()
        list.add(VarInsnNode(Opcodes.ALOAD, 0))
        list.add(InsnNode(Opcodes.ICONST_1))
        list.add(FieldInsnNode(Opcodes.PUTFIELD, "net/minecraftforge/client/model/ModelLoader", "isLoading", "Z"))
        val hook = getInstructionsWithTryCatchNodes {
            of(ModelLoaderHook::setupModelRegistry)
            target(methodNode)
            param(0)
            param {
                aload_0
                getfield(
                    "net/minecraftforge/client/model/ModelLoader",
                    "missingModel",
                    "net/minecraftforge/client/model/IModel"
                )
            }
            param {
                aload_0
                getfield("net/minecraftforge/client/model/ModelLoader", "stateModels", Map::class)
            }
            param {
                aload_0
                getfield("net/minecraftforge/client/model/ModelLoader", "textures", Set::class)
            }
            keepReturns
        }
        list.add(hook.first)
        methodNode.tryCatchBlocks.addAll(hook.second)
        return list
    }

    private fun createCallLoadItems() = assembleBlock {
        aload_0
        invokespecial("net/minecraftforge/client/model/ModelLoader", "loadItems", void)
        _return
    }.first

    private fun createCallLoadBlocks() = assembleBlock {
        aload_0
        invokespecial("net/minecraftforge/client/model/ModelLoader", "loadBlocks", void)
        _return
    }.first

    private fun clearMemory() = assembleBlock {
        aload_0
        getfield("net/minecraftforge/client/model/ModelLoader", "loadingExceptions", Map::class)
        invokeinterface(Map::class, "clear", void)
        aload_0
        getfield("net/minecraftforge/client/model/ModelLoader", "missingVariants", Set::class)
        invokeinterface(Set::class, "clear", void)
    }.first
}