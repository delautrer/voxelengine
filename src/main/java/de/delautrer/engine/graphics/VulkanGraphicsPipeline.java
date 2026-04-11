package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanGraphicsPipeline {
    private final VulkanContext context;
    private long descriptorSetLayout;
    private long pipelineLayout;
    private long graphicsPipeline;    // Für Stein, Erde, Gras
    private long transparentPipeline; // Für Wasser

    public VulkanGraphicsPipeline(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        createPipelines(swapchain, renderPass);
    }

    private void createPipelines(VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/vert.spv");
            ByteBuffer fragShaderCode = ShaderUtils.readSPIRV("src/main/resources/shaders/frag.spv");

            long vertModule = createModule(vertShaderCode, stack);
            long fragModule = createModule(fragShaderCode, stack);

            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK10.VK_SHADER_STAGE_VERTEX_BIT).module(vertModule).pName(stack.UTF8("main"));
            stages.get(1).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT).module(fragModule).pName(stack.UTF8("main"));

            // --- ÄNDERUNG FÜR TEXTURE ARRAYS ---
            // Vertex Input (Stride ist jetzt 10: x,y,z, r,g,b,a, u,v, layer)
            VkVertexInputBindingDescription.Buffer binding = VkVertexInputBindingDescription.calloc(1, stack);
            binding.binding(0).stride(10 * Float.BYTES).inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);

            // Wir brauchen jetzt 4 Attribute (vorher 3)
            VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(4, stack);
            attributes.get(0).binding(0).location(0).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            attributes.get(1).binding(0).location(1).format(VK10.VK_FORMAT_R32G32B32A32_SFLOAT).offset(3 * Float.BYTES);
            attributes.get(2).binding(0).location(2).format(VK10.VK_FORMAT_R32G32_SFLOAT).offset(7 * Float.BYTES);
            // NEU: inTexLayer (1 Float) an Position 9
            attributes.get(3).binding(0).location(3).format(VK10.VK_FORMAT_R32_SFLOAT).offset(9 * Float.BYTES);

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(binding)
                    .pVertexAttributeDescriptions(attributes);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).viewportCount(1).scissorCount(1);
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO).polygonMode(VK10.VK_POLYGON_MODE_FILL).cullMode(VK10.VK_CULL_MODE_BACK_BIT).frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE).lineWidth(1.0f);
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

            // Descriptor Layout & Push Constants
            createLayouts(stack);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(stages).pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly).pViewportState(viewportState).pRasterizationState(rasterizer).pMultisampleState(multisampling).layout(pipelineLayout).renderPass(renderPass.getHandle()).subpass(0);

            // DYNAMISCHE STATES
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).pDynamicStates(stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR));
            pipelineInfo.pDynamicState(dynamicState);

            // --- 1. OPAQUE PIPELINE ---
            VkPipelineDepthStencilStateCreateInfo depthOpaque = VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO).depthTestEnable(true).depthWriteEnable(true).depthCompareOp(VK10.VK_COMPARE_OP_LESS);
            VkPipelineColorBlendAttachmentState.Buffer blendOpaque = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            blendOpaque.colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT).blendEnable(false);
            VkPipelineColorBlendStateCreateInfo colorBlendOpaque = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(blendOpaque);

            pipelineInfo.pDepthStencilState(depthOpaque).pColorBlendState(colorBlendOpaque);
            LongBuffer pOpaque = stack.mallocLong(1);
            VK10.vkCreateGraphicsPipelines(context.getDevice(), VK10.VK_NULL_HANDLE, pipelineInfo, null, pOpaque);
            graphicsPipeline = pOpaque.get(0);

            // --- 2. TRANSPARENT PIPELINE (WASSER) ---
            VkPipelineDepthStencilStateCreateInfo depthTrans = VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO).depthTestEnable(true).depthWriteEnable(false).depthCompareOp(VK10.VK_COMPARE_OP_LESS);
            VkPipelineColorBlendAttachmentState.Buffer blendTrans = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            blendTrans.colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(true)
                    .srcColorBlendFactor(VK10.VK_BLEND_FACTOR_SRC_ALPHA)
                    .dstColorBlendFactor(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    .colorBlendOp(VK10.VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(VK10.VK_BLEND_OP_ADD);

            VkPipelineColorBlendStateCreateInfo colorBlendTrans = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(blendTrans);

            pipelineInfo.pDepthStencilState(depthTrans).pColorBlendState(colorBlendTrans);
            LongBuffer pTrans = stack.mallocLong(1);
            VK10.vkCreateGraphicsPipelines(context.getDevice(), VK10.VK_NULL_HANDLE, pipelineInfo, null, pTrans);
            transparentPipeline = pTrans.get(0);

            VK10.vkDestroyShaderModule(context.getDevice(), vertModule, null);
            VK10.vkDestroyShaderModule(context.getDevice(), fragModule, null);
        }
    }

    private void createLayouts(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(1, stack);
        binding.get(0).binding(0).descriptorCount(1).descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).stageFlags(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(binding);
        LongBuffer pLayout = stack.mallocLong(1);
        VK10.vkCreateDescriptorSetLayout(context.getDevice(), layoutInfo, null, pLayout);
        descriptorSetLayout = pLayout.get(0);

        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
        pushConstantRange.stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT).offset(0).size(68);

        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(stack.longs(descriptorSetLayout)).pPushConstantRanges(pushConstantRange);
        VK10.vkCreatePipelineLayout(context.getDevice(), pipelineLayoutInfo, null, pLayout);
        pipelineLayout = pLayout.get(0);
    }

    private long createModule(ByteBuffer code, MemoryStack stack) {
        VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(code);
        LongBuffer pMod = stack.mallocLong(1);
        VK10.vkCreateShaderModule(context.getDevice(), info, null, pMod);
        return pMod.get(0);
    }

    public long getHandle() { return graphicsPipeline; }
    public long getTransparentHandle() { return transparentPipeline; }
    public long getPipelineLayout() { return pipelineLayout; }
    public long getDescriptorSetLayout() { return descriptorSetLayout; }

    public void cleanup() {
        VK10.vkDestroyPipeline(context.getDevice(), graphicsPipeline, null);
        VK10.vkDestroyPipeline(context.getDevice(), transparentPipeline, null);
        VK10.vkDestroyPipelineLayout(context.getDevice(), pipelineLayout, null);
        VK10.vkDestroyDescriptorSetLayout(context.getDevice(), descriptorSetLayout, null);
    }
}