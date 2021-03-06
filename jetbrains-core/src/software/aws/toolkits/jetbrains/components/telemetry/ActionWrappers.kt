// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.components.telemetry

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import software.aws.toolkits.core.telemetry.TelemetryNamespace
import software.aws.toolkits.jetbrains.services.telemetry.TelemetryService
import javax.swing.Icon

object ToolkitActionPlaces {
    val EXPLORER_WINDOW = "ExplorerToolWindow"
}

// Constructor signatures:
//  public AnAction(){
//  }
//  public AnAction(Icon icon){
//    this(null, null, icon);
//  }
//  public AnAction(@Nullable String text) {
//    this(text, null, null);
//  }
//  public AnAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
//    <logic>
//  }
abstract class AnActionWrapper : TelemetryNamespace, AnAction {
    constructor(): super()
    constructor(text: String? = null, description: String? = null, icon: Icon? = null):
        super(text, description, icon)

    /**
     * Consumers should use doActionPerformed(e: AnActionEvent)
     */
    final override fun actionPerformed(e: AnActionEvent) {
        doActionPerformed(e)
        telemetry.record(getNamespace()) {
            datum(e.place) {
                count()
            }
        }
    }

    abstract fun doActionPerformed(e: AnActionEvent)

    companion object {
        protected val telemetry = TelemetryService.getInstance()
    }
}

abstract class ComboBoxActionWrapper : TelemetryNamespace, ComboBoxAction() {
    /**
     * Consumers should use doActionPerformed(e: AnActionEvent)
     */
    final override fun actionPerformed(e: AnActionEvent) {
        doActionPerformed(e)
        telemetry.record(getNamespace()) {
            datum(e.place) {
                count()
            }
        }
    }

    open fun doActionPerformed(e: AnActionEvent) = super.actionPerformed(e)

    companion object {
        protected val telemetry = TelemetryService.getInstance()
    }
}

abstract class ToogleActionWrapper : TelemetryNamespace, ToggleAction {
    constructor(): super()
    constructor(text: String? = null, description: String? = null, icon: Icon? = null):
        super(text, description, icon)

    // this will be repeatedly called by the IDE, so we likely do not want telemetry on this,
    // but keeping this to maintain API consistency
    final override fun isSelected(e: AnActionEvent): Boolean = doIsSelected(e)

    final override fun setSelected(e: AnActionEvent, state: Boolean) {
        doSetSelected(e, state)
        telemetry.record(getNamespace()) {
            datum(e.place) {
                count()
            }
        }
    }

    abstract fun doIsSelected(e: AnActionEvent): Boolean

    abstract fun doSetSelected(e: AnActionEvent, state: Boolean)

    companion object {
        protected val telemetry = TelemetryService.getInstance()
    }
}