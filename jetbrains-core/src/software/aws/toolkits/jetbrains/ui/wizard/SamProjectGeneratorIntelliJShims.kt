// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.ui.wizard

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectTemplatesFactory
import icons.AwsIcons
import software.aws.toolkits.jetbrains.services.lambda.RuntimeGroup
import software.aws.toolkits.jetbrains.services.lambda.execution.sam.SamCommon
import software.aws.toolkits.jetbrains.services.lambda.runtimeGroup
import software.aws.toolkits.resources.message

// IntelliJ shim requires a ModuleBuilder
// UI is centralized in generator and is passed in to have access to UI elements
class SamProjectBuilder(private val generator: SamProjectGenerator) : ModuleBuilder() {
    // hide this from the new project menu
    override fun isAvailable() = false

    // dummy type to fulfill the interface
    override fun getModuleType() = AwsModuleType.INSTANCE

    // IntelliJ create commit step
    override fun setupRootModel(rootModel: ModifiableRootModel) {
        // sdk config deviates here since we're not storing information in the module builder like other standard
        // IntelliJ project wizards
        val sdk = generator.settings.sdk
        // project sdk
        ProjectRootManager.getInstance(rootModel.project).projectSdk = sdk
        // module sdk
        rootModel.inheritSdk()

        val selectedRuntime = generator.settings.runtime
        val moduleType = selectedRuntime.runtimeGroup?.getModuleType() ?: ModuleType.EMPTY

        rootModel.module.setModuleType(moduleType.id)
        val project = rootModel.project

        val contentEntry: ContentEntry = doAddContentEntry(rootModel) ?: throw Exception(message("sam.init.error.no.project.basepath"))
        val outputDir: VirtualFile = contentEntry.file ?: throw Exception(message("sam.init.error.no.virtual.file"))

        generator.settings.template.build(selectedRuntime, outputDir)

        SamCommon.excludeSamDirectory(outputDir, rootModel)

        if (selectedRuntime.runtimeGroup == RuntimeGroup.PYTHON) {
            SamCommon.setSourceRoots(outputDir, project, rootModel)
        }
    }

    // IntelliJ wizard steps would go here. We will have to build a custom wizard in SamProjectRuntimeSelectionStep
    override fun createFinishingSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> =
        super.createFinishingSteps(wizardContext, modulesProvider)

    // add things
    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep? {
        generator.createPeer().buildUI(settingsStep)

        // need to return an object with validate() implemented for validation
        return object : ModuleWizardStep() {
            override fun getComponent() = null

            override fun updateDataModel() {
                generator.peer.sdkPanel.ensureSdk()
            }

            @Throws(ConfigurationException::class)
            override fun validate(): Boolean {
                val info = generator.peer.validate()
                if (info != null) throw ConfigurationException(info.message)

                return true
            }
        }
    }
}

class NullBuilder : ModuleBuilder() {
    // hide this from the new project menu
    override fun isAvailable() = false

    override fun getModuleType(): ModuleType<*> = AwsModuleType.INSTANCE

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel?) {}
}

class AwsModuleType : ModuleType<ModuleBuilder>(ID) {
    override fun createModuleBuilder() = NullBuilder()

    override fun getName() = ID

    override fun getDescription() = message("aws.description")

    override fun getNodeIcon(isOpened: Boolean) = AwsIcons.Logos.AWS

    companion object {
        const val ID = "AWS"
        val INSTANCE: ModuleType<*> = ModuleTypeManager.getInstance().findByID(ID)
    }
}

class SamProjectGeneratorIntelliJAdapter : ProjectTemplatesFactory() {
    // pull in AWS project types here
    override fun createTemplates(group: String?, context: WizardContext?) = arrayOf(SamProjectGenerator())

    override fun getGroupIcon(group: String?) = AwsIcons.Logos.AWS

    override fun getGroups() = arrayOf("AWS")
}