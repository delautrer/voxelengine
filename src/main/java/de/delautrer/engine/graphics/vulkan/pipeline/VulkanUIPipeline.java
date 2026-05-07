package de.delautrer.engine.graphics.vulkan.pipeline;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanUIPipeline {

    private final VulkanContext context;
    private long pipelineLayout;
    private long graphicsPipeline;
    private long descriptorSetLayout;

    public VulkanUIPipeline(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
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
            ByteBuffer vertShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/ui.vert.spv");
            ByteBuffer fragShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/ui.frag.spv");

            long vertShaderModule = createShaderModule(vertShaderCode, stack);
            long fragShaderModule = createShaderModule(fragShaderCode, stack);

            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            shaderStages.get(0).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStages.get(0).stage(VK10.VK_SHADER_STAGE_VERTEX_BIT);
            shaderStages.get(0).module(vertShaderModule);
            shaderStages.get(0).pName(entryPoint);

            shaderStages.get(1).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            shaderStages.get(1).stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);
            shaderStages.get(1).module(fragShaderModule);
            shaderStages.get(1).pName(entryPoint);

            VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1,
                    stack);
            bindingDescription.binding(0);
            bindingDescription.stride(8 * Float.BYTES);
            bindingDescription.inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(3,
                    stack);
            attributeDescriptions.get(0).binding(0).location(0).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            attributeDescriptions.get(1).binding(0).location(1).format(VK10.VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(3 * Float.BYTES);
            attributeDescriptions.get(2).binding(0).location(2).format(VK10.VK_FORMAT_R32G32_SFLOAT)
                    .offset(6 * Float.BYTES);

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(bindingDescription);
            vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f).y(0.0f).width((float) swapchain.getExtent().width())
                    .height((float) swapchain.getExtent().height()).minDepth(0.0f).maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(swapchain.getExtent());

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.viewportCount(1).pViewports(viewport);
            viewportState.scissorCount(1).pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false).rasterizerDiscardEnable(false).polygonMode(VK10.VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f).cullMode(VK10.VK_CULL_MODE_NONE).frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE);

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

            // --- Z-INDEX: Tiefentest AN und auf GREATER_OR_EQUAL gesetzt ---
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            // depthStencil.depthTestEnable(true).depthWriteEnable(true);
            depthStencil.depthTestEnable(false).depthWriteEnable(false);
            depthStencil.depthCompareOp(VK10.VK_COMPARE_OP_GREATER_OR_EQUAL);

            // Blending-Attachment (hier deaktivieren wir das klassische Alpha-Blending,
            // damit LogicOp greift)
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState
                    .calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT |
                            VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(true);

            // Neue Farbe * Alpha + Alte Farbe * (1 - Alpha)
            colorBlendAttachment.srcColorBlendFactor(VK10.VK_BLEND_FACTOR_SRC_ALPHA);
            colorBlendAttachment.dstColorBlendFactor(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
            colorBlendAttachment.colorBlendOp(VK10.VK_BLEND_OP_ADD);
            colorBlendAttachment.srcAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE);
            colorBlendAttachment.dstAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ZERO);
            colorBlendAttachment.alphaBlendOp(VK10.VK_BLEND_OP_ADD);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);

            colorBlending.attachmentCount(1);
            colorBlending.pAttachments(colorBlendAttachment);

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            bindings.get(0).binding(0).descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .stageFlags(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            VK10.vkCreateDescriptorSetLayout(context.getDevice(), layoutInfo, null, pDescriptorSetLayout);
            descriptorSetLayout = pDescriptorSetLayout.get(0);

            VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
            pushConstantRange.stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT).offset(0).size(64);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout)).pPushConstantRanges(pushConstantRange);

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            VK10.vkCreatePipelineLayout(context.getDevice(), pipelineLayoutInfo, null, pPipelineLayout);
            pipelineLayout = pPipelineLayout.get(0);

            IntBuffer dynamicStates = stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR);
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).pDynamicStates(dynamicStates);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.stageCount(2).pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo).pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState).pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling).pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending).pDynamicState(dynamicState);
            pipelineInfo.layout(pipelineLayout).renderPass(renderPass.getHandle()).subpass(0);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            VK10.vkCreateGraphicsPipelines(context.getDevice(), VK10.VK_NULL_HANDLE, pipelineInfo, null,
                    pGraphicsPipeline);
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

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public void cleanup() {
        VK10.vkDestroyPipeline(context.getDevice(), graphicsPipeline, null);
        VK10.vkDestroyPipelineLayout(context.getDevice(), pipelineLayout, null);
        VK10.vkDestroyDescriptorSetLayout(context.getDevice(), descriptorSetLayout, null);
    }
}
