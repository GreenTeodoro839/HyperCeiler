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
package com.sevtinge.hyperceiler.libhook.utils.hookapi

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod

object StateFlowHelper {
    private const val TAG = "StateFlowHelper"

     private val STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.StateFlow")
    }

    private val STATE_FLOW_KT by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.StateFlowKt")
    }

    private val READONLY_STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.ReadonlyStateFlow")
    }

    private val MUTABLE_STATE_FLOW by lazy {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("kotlinx.coroutines.flow.MutableStateFlow")
    }

    private val READONLY_STATE_FLOW_CONSTRUCTOR by lazy {
        if (isMoreAndroidVersion(36)) {
            READONLY_STATE_FLOW.getConstructor(MUTABLE_STATE_FLOW)
        } else {
            READONLY_STATE_FLOW.getConstructor(STATE_FLOW)
        }
    }

    @JvmStatic
    fun newStateFlow(initValue: Any?): Any {
        return STATE_FLOW_KT.callStaticMethod("MutableStateFlow", initValue) as Any
    }

    @JvmStatic
    fun newReadonlyStateFlow(initValue: Any?): Any {
        return READONLY_STATE_FLOW_CONSTRUCTOR.newInstance(newStateFlow(initValue))
    }

    @JvmStatic
    fun setStateFlowValue(stateFlow: Any?, value: Any?) {
        stateFlow ?: return

        val target = when (stateFlow::class.java.simpleName) {
            "ReadonlyStateFlow" -> findMutableDelegate(stateFlow)
            "StateFlowImpl" -> stateFlow
            else -> null
        } ?: return

        runCatching { target.callMethod("setValue", value) }.onFailure {
            XposedLog.e(TAG, "failed to set value on ${target::class.java.name}: $it")
        }
    }

    /**
     * ReadonlyStateFlow 只是个委托壳，真正可写的 flow 在它的委托字段里。
     * 该字段的声明类型各版本不一样（MutableStateFlow / StateFlow），R8 还会把字段名混淆掉，
     * 所以直接按字段的实际值找，不依赖名字和声明类型。
     *
     * 这个方法跑在 SystemUI 的 flow collector 回调里，**一旦抛出去就是 SystemUI 崩溃循环**，
     * 所以找不到就返回 null，绝不往外抛。
     */
    private fun findMutableDelegate(stateFlow: Any): Any? {
        for (field in stateFlow::class.java.declaredFields) {
            val delegate = runCatching {
                field.isAccessible = true
                field.get(stateFlow)
            }.getOrNull() ?: continue
            if (MUTABLE_STATE_FLOW.isInstance(delegate)) return delegate
        }
        XposedLog.e(TAG, "no mutable delegate in ${stateFlow::class.java.name}")
        return null
    }

    @JvmStatic
    fun getStateFlowValue(stateFlow: Any?): Any? {
        return stateFlow?.callMethod("getValue")
    }
}
