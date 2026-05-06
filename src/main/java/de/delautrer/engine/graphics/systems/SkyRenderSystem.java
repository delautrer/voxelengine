package de.delautrer.engine.graphics.systems;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.RenderPacket;
import de.delautrer.engine.graphics.ShaderUtils;
import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import de.delautrer.engine.graphics.vulkan.core.VulkanRenderPass;
import de.delautrer.engine.graphics.vulkan.core.VulkanSwapchain;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class SkyRenderSystem implements IRenderSystem {

    private final VulkanContext context;
    private long pipelineLayout;
    private long pipeline;

    public SkyRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        createPipeline(swapchain, renderPass);
    }

    private void createPipeline(VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/sky.vert.spv");
            ByteBuffer fragShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/sky.frag.spv");

            long vertModule = createShaderModule(vertShaderCode, stack);
            long fragModule = createShaderModule(fragShaderCode, stack);

            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertModule).pName(stack.UTF8("main"));
            stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragModule).pName(stack.UTF8("main"));

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).viewportCount(1).scissorCount(1);
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO).polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE).lineWidth(1.0f);
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // Tiefentest für Himmel DEAKTIVIEREN
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(false)
                    .depthWriteEnable(false)
                    .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT).blendEnable(false);
            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(colorBlendAttachment);
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPushConstantRange.Buffer pushConstant = VkPushConstantRange.calloc(1, stack);
            pushConstant.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                    .offset(0).size(128);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pPushConstantRanges(pushConstant); // WICHTIG: SetLayouts entfällt komplett!

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            vkCreatePipelineLayout(context.getDevice(), pipelineLayoutInfo, null, pPipelineLayout);
            pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages).pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly).pViewportState(viewportState)
                    .pRasterizationState(rasterizer).pMultisampleState(multisampling).pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending).pDynamicState(dynamicState).layout(pipelineLayout)
                    .renderPass(renderPass.getHandle()).subpass(0);

            LongBuffer pPipeline = stack.mallocLong(1);
            vkCreateGraphicsPipelines(context.getDevice(), VK_NULL_HANDLE, pipelineInfo, null, pPipeline);
            pipeline = pPipeline.get(0);

            vkDestroyShaderModule(context.getDevice(), vertModule, null);
            vkDestroyShaderModule(context.getDevice(), fragModule, null);
        }
    }

    private long createShaderModule(ByteBuffer code, MemoryStack stack) {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(code);
        LongBuffer pShaderModule = stack.mallocLong(1);
        vkCreateShaderModule(context.getDevice(), createInfo, null, pShaderModule);
        return pShaderModule.get(0);
    }

    @Override
    public void render(VkCommandBuffer commandBuffer, RenderPacket packet) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

        // --- DIREKT IN DEN COMMAND BUFFER SCHREIBEN (100% sicher) ---
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pc = stack.malloc(128); // <--- HIER von 112 auf 128 erhöhen!

            // Wir kombinieren Proj und View zu EINER Matrix = Bessere Performance!
            Matrix4f view = new Matrix4f(packet.view);
            view.setTranslation(0, 0, 0);
            Matrix4f invVP = new Matrix4f(packet.proj).mul(view).invert();

            // 1. Matrix einfügen (Bytes 0 bis 63)
            invVP.get(0, pc);

            // 2. Sonne einfügen (Bytes 64 bis 79)
            pc.putFloat(64, packet.sunDirection.x);
            pc.putFloat(68, packet.sunDirection.y);
            pc.putFloat(72, packet.sunDirection.z);
            pc.putFloat(76, 0.0f); // Padding

            // 3. Zenith Color (Bytes 80 bis 95)
            pc.putFloat(80, packet.skyR);
            pc.putFloat(84, packet.skyG);
            pc.putFloat(88, packet.skyB);
            pc.putFloat(92, 0.0f); // Padding

            // 4. Horizon Color (Bytes 96 bis 111)
            pc.putFloat(96, Math.min(packet.skyR * 1.5f, 1.0f));
            pc.putFloat(100, Math.min(packet.skyG * 1.5f, 1.0f));
            pc.putFloat(104, Math.min(packet.skyB * 1.5f, 1.0f));
            pc.putFloat(108, 0.0f); // Padding

            // 5. isUnderwater & globalLight
            pc.putFloat(112, packet.isUnderwater ? 1.0f : 0.0f);
            pc.putFloat(116, packet.globalLight);

            // Sofort an die Grafikkarte absenden! Kein Buffer-Mapping nötig.
            vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, pc);
        }

        vkCmdDraw(commandBuffer, 6, 1, 0, 0);
    }

    @Override
    public void cleanup() {
        vkDestroyPipeline(context.getDevice(), pipeline, null);
        vkDestroyPipelineLayout(context.getDevice(), pipelineLayout, null);
    }
}
