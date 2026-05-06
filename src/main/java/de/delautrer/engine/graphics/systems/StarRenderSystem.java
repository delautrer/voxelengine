package de.delautrer.engine.graphics.systems;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

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

public class StarRenderSystem implements IRenderSystem {

    private final VulkanContext context;
    private long pipelineLayout;
    private long pipeline;

    public StarRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        createPipeline(swapchain, renderPass);
    }

    private void createPipeline(VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/star.vert.spv");
            ByteBuffer fragShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/star.frag.spv");

            long vertModule = createShaderModule(vertShaderCode, stack);
            long fragModule = createShaderModule(fragShaderCode, stack);

            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertModule).pName(stack.UTF8("main"));
            stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragModule).pName(stack.UTF8("main"));

            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1, stack);
            bindingDescription.binding(0).stride(5 * Float.BYTES).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescription = VkVertexInputAttributeDescription.calloc(2, stack);
            attributeDescription.get(0).binding(0).location(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            attributeDescription.get(1).binding(0).location(1).format(VK_FORMAT_R32G32_SFLOAT).offset(3 * Float.BYTES);

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(bindingDescription)
                    .pVertexAttributeDescriptions(attributeDescription);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).viewportCount(1).scissorCount(1);
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO).polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE).lineWidth(1.0f);
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(false)
                    .depthWriteEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(true)
                    .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                    .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    .colorBlendOp(VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(VK_BLEND_OP_ADD);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(colorBlendAttachment);
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPushConstantRange.Buffer pushConstant = VkPushConstantRange.calloc(1, stack);
            pushConstant.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT).offset(0).size(80);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pPushConstantRanges(pushConstant);

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
        if (packet.starMesh == null || ((VulkanMesh)packet.starMesh).getIndexCount() == 0 || packet.starAlpha <= 0.0f) {
            return;
        }

        VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            float baseAngle = ((packet.timeOfDay - 6.0f) / 24.0f) * (float) (Math.PI * 2.0);

            // HIER GEÄNDERT: Sterne drehen sich nur noch mit 5% der Normalgeschwindigkeit
            float angle = baseAngle * 0.05f;

            Matrix4f view = new Matrix4f(packet.view);
            view.m30(0); view.m31(0); view.m32(0);

            Matrix4f starMvp = new Matrix4f(packet.proj)
                    .mul(view)
                    .rotateZ(angle);

            ByteBuffer pc = stack.malloc(80);
            starMvp.get(0, pc);
            pc.putFloat(64, packet.starAlpha);
            pc.putFloat(68, packet.timeOfDay);

            VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pc);

            VK10.vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(((VulkanMesh)packet.starMesh).getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(commandBuffer, ((VulkanMesh)packet.starMesh).getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

            VK10.vkCmdDrawIndexed(commandBuffer, ((VulkanMesh)packet.starMesh).getIndexCount(), 1, 0, 0, 0);
        }
    }

    @Override
    public void cleanup() {
        vkDestroyPipeline(context.getDevice(), pipeline, null);
        vkDestroyPipelineLayout(context.getDevice(), pipelineLayout, null);
    }
}
