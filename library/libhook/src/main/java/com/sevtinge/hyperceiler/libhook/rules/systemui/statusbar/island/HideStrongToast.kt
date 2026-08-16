/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island

import android.os.Bundle
import android.widget.FrameLayout
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.hookapi.LazyClass.NewStrongToast
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook


object HideStrongToast : BaseHook() {
    override fun init() {
        if (isMoreHyperOSVersion(4f)) {
            loadClass("com.android.systemui.devicenotification.listener.DeviceNotificationListenerImpl")
                .findMethod {
                    name("setStatus")
                    parameterTypes(Int::class.java, String::class.java, Bundle::class.java)
                }.createBeforeHook { param ->
                    if (param.args[1] == "strong_toast_action") {
                        param.result = null
                    }
                }
            return
        }

        NewStrongToast!!.findMethod { name("onAttachedToWindow") }.createAfterHook {
            val strongToastLayout = it.thisObject as FrameLayout
            strongToastLayout.viewTreeObserver.addOnPreDrawListener {
                return@addOnPreDrawListener false
            }
        }
    }
}
