package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanLinePipeline {

    private final VulkanContext context;
    private long pipelineLayout;
    private long graphicsPipeline;

    public VulkanLinePipeline(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        createGraphicsPipeline(swapchain, renderPass);
    }

    private long createShaderModule(ByteBuffer spirvCode, MemoryStack stack) {
        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
        createInfo.pCode(spirvCode);

        LongBuffer pShaderModule = stack.mallocLong(1);
        if (VK10.vkCreateShaderModule(context.getDevice(), createInfo, null, pShaderModule) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to create shader module");
        }

        return pShaderModule.get(0);
    }

    private void createGraphicsPipeline(VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/line.vert.spv");
            ByteBuffer fragShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/line.frag.spv");

            long vertShaderModule = createShaderModule(vertShaderCode, stack);
            long fragShaderModule = createShaderModule(fragShaderCode, stack);

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);
            vertShaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK10.VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);
            fragShaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1, stack);
            bindingDescription.binding(0);
            bindingDescription.stride(3 * Float.BYTES); // Just position (x, y, z)
            bindingDescription.inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1, stack);

            attributeDescriptions.get(0).binding(0);
            attributeDescriptions.get(0).location(0);
            attributeDescriptions.get(0).format(VK10.VK_FORMAT_R32G32B32_SFLOAT);
            attributeDescriptions.get(0).offset(0);

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(bindingDescription);
            vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST); // Draw lines!
            inputAssembly.primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) swapchain.getExtent().width());
            viewport.height((float) swapchain.getExtent().height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(swapchain.getExtent());

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.viewportCount(1);
            viewportState.pViewports(viewport);
            viewportState.scissorCount(1);
            viewportState.pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK10.VK_POLYGON_MODE_LINE); // Line mode!
            rasterizer.lineWidth(3.0f); // Make the outline bold
            rasterizer.cullMode(VK10.VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(true);
            depthStencil.depthWriteEnable(true);
            depthStencil.depthCompareOp(VK10.VK_COMPARE_OP_LESS);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.stencilTestEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK10.VK_COLOR_COMPONENT_R_BIT |
                            VK10.VK_COLOR_COMPONENT_G_BIT |
                            VK10.VK_COLOR_COMPONENT_B_BIT |
                            VK10.VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(true);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.attachmentCount(1);
            colorBlending.pAttachments(colorBlendAttachment);

            VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
            pushConstantRange.stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT);
            pushConstantRange.offset(0);
            pushConstantRange.size(64);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(null); // No descriptor sets needed
            pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            if (VK10.vkCreatePipelineLayout(context.getDevice(), pipelineLayoutInfo, null, pPipelineLayout) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }
            pipelineLayout = pPipelineLayout.get(0);

            IntBuffer dynamicStates = stack.ints(
                    VK10.VK_DYNAMIC_STATE_VIEWPORT,
                    VK10.VK_DYNAMIC_STATE_SCISSOR
            );

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicState.pDynamicStates(dynamicStates);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.stageCount(2);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicState);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass.getHandle());
            pipelineInfo.subpass(0);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            if (VK10.vkCreateGraphicsPipelines(context.getDevice(), VK10.VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }
            graphicsPipeline = pGraphicsPipeline.get(0);

            VK10.vkDestroyShaderModule(context.getDevice(), vertShaderModule, null);
            VK10.vkDestroyShaderModule(context.getDevice(), fragShaderModule, null);
        }
    }

    public long getHandle() {
        return graphicsPipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public void cleanup() {
        VK10.vkDestroyPipeline(context.getDevice(), graphicsPipeline, null);
        VK10.vkDestroyPipelineLayout(context.getDevice(), pipelineLayout, null);
    }
}