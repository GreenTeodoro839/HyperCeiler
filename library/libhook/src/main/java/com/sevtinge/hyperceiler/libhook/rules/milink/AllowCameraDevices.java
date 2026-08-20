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
package com.sevtinge.hyperceiler.libhook.rules.milink;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AllowCameraDevices extends BaseHook {
    @Override
    public void init() {
        Class<?> rulesConfig = findClassIfExists("com.xiaomi.vtcamera.cloud.RulesConfig");
        Class<?> appEntityInfo = findClassIfExists("com.xiaomi.camera.companion.AppEntityInfo");
        if (rulesConfig == null || appEntityInfo == null) return;

        IMethodHook hook = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        };

        Method shortAuthorised = findMethodExactIfExists(
            rulesConfig, "isAuthorised", String.class, int.class, appEntityInfo);
        Method fullAuthorised = findMethodExactIfExists(
            rulesConfig, "isAuthorised", String.class, int.class, String.class,
            List.class, String.class, int.class, long.class, String.class);
        if (shortAuthorised != null) hookMethod(shortAuthorised, hook);
        if (fullAuthorised != null) hookMethod(fullAuthorised, hook);
    }
}
