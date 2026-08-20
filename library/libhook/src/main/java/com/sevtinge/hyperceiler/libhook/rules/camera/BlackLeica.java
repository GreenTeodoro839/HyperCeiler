/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.camera;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class BlackLeica extends BaseHook {
    private Class<?> mTextColorMakerClazz;
    private Method mWaterMakerLeicaMethod;
    private Method mTextPainterMethod;
    private Method mTextColorMakerMethod;
    private Field mDescStringColorField;
    private Field mLeicaPendantColorField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mTextColorMakerClazz = requiredMember("TextColorMakerClazz", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("get(ColorSpace.Named.SRGB)")
                        )).singleOrNull();
                return clazzData;
            }
        });

        mWaterMakerLeicaMethod = requiredMember("WaterMakerLeica", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("deviceNameLengthType: ")
                        .returnType(Bitmap.class)
                    )).singleOrNull();
                return methodData;
            }
        });

        // Class<?> clazz1 = method1.getClass();
        mTextPainterMethod = requiredMember("TextPainter", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .paramTypes(Typeface.class, float.class, int.class, Paint.Align.class, float.class)
                                .returnType(TextPaint.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        mTextColorMakerMethod = requiredMember("TextColorMaker", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(mTextColorMakerClazz)
                                .paramTypes(int.class)
                                .returnType(mTextColorMakerClazz)
                        )).singleOrNull();
                return methodData;
            }
        });
        mDescStringColorField = requiredMember("DescStringColor", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                // 权宜之计：该颜色字段没有稳定语义特征；混淆名已在当前真实 dex 中核实，宿主更新后可能失效。
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass("mu.b")
                                .name("a")
                                .type(int.class)
                        )).singleOrNull();
                return fieldData;
            }
        });
        mLeicaPendantColorField = requiredMember("LeicaPendantColor", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("#33000000", "ISWN")
                                )
                                .type(int.class)
                        )).singleOrNull();
                return fieldData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        com.sevtinge.hyperceiler.libhook.base.BaseHook.setStaticIntField(mDescStringColorField.getDeclaringClass(), mDescStringColorField.getName(), Color.parseColor("#8CFFFFFF"));
        com.sevtinge.hyperceiler.libhook.base.BaseHook.setStaticIntField(mLeicaPendantColorField.getDeclaringClass(), mLeicaPendantColorField.getName(), Color.parseColor("#33FFFFFF"));
        hookMethod(mWaterMakerLeicaMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                hookMethod(mTextPainterMethod, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        if ((int) param.getArgs()[2] == -16777216) param.getArgs()[2] = -1;
                    }
                });
                hookMethod(mTextColorMakerMethod, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.getArgs()[0] = 1048576;
                    }
                });
            }
        });
    }
}
