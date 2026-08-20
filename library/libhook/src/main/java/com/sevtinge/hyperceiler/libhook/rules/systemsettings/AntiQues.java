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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AntiQues extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.DeviceNameCheckManager",
            "getDeviceNameCheckResult", Context.class, String.class, int.class,
            "com.android.settings.DeviceNameCheckManager$GetResultSuccessCallback", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Object callback = param.getArgs()[3];
                if (callback != null) {
                    Method resolveResult = findMethodExactIfExists(
                        callback.getClass(), "resolveResult", String.class);
                    if (resolveResult != null) {
                        try {
                            invokeOriginalMethod(resolveResult, callback, param.getArgs()[1]);
                        } catch (Throwable t) {
                            XposedLog.e(TAG, getPackageName(), "Cannot bypass device name check", t);
                        }
                    }
                }
                param.setResult(null);
            }
        });

        findAndHookMethod("com.android.settings.wifi.EditTetherFragment",
            "isSoftApSsidchanged", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isSupportNameComplianceCheck", Context.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.settings.bluetooth.MiuiBTUtils", "isInternationalBuild", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });
    }
}
