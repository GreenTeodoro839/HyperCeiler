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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.WeatherView
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

// 经典控制中心添加天气信息
object OldWeather : BaseHook() {
    private val isDisplayCity by lazy {
        PrefsBridge.getBoolean("system_ui_control_center_show_weather_city")
    }

    @SuppressLint("DiscouragedApi")
    override fun init() {
        var mWeatherView: TextView?
        loadClass("com.android.systemui.qs.MiuiQSHeaderView").findMethod { name("onFinishInflate") }.createAfterHook {
                val viewGroup = it.thisObject as ViewGroup
                val context = viewGroup.context
                val shortcut = viewGroup.findViewById<View>(
                    context.getIdByName("notification_shade_shortcut")
                ) ?: return@createAfterHook
                val parent = shortcut.parent as ViewGroup

                mWeatherView = WeatherView(context, isDisplayCity).apply {
                    id = View.generateViewId()
                    setTextAppearance(
                        context.getIdByName(
                            "TextAppearance.StatusBar.Expanded.Clock.QuickSettingDate",
                            "style"
                        )
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = dp2px(5f)
                    }

                    setOnClickListener {
                        startWeatherApp()
                    }
                }
                parent.addView(mWeatherView, parent.indexOfChild(shortcut))
            }
    }
}
