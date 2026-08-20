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
package com.sevtinge.hyperceiler.libhook.utils.api;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Android 17 起 ART 拒绝一切反射写入 static final 字段的请求，
 * {@code Field.set} 会直接抛 {@code IllegalAccessException: Cannot set ... static final field ...}。
 *
 * <p>但 {@code sun.misc.Unsafe} 的 put 系列不做任何访问检查。静态字段的
 * {@code Field.offset} 就是它在 {@code mirror::Class} 对象里的偏移量，
 * 于是 {@code putObject(declaringClass, offset, value)} 能绕过限制，
 * 且真实字节码 {@code sget} 读到的就是新值。
 */
public final class FinalFieldUtils {
    private static final String TAG = "FinalFieldUtils";

    private static boolean sPrepared;
    private static Object sUnsafe;
    private static Field sOffsetField;
    private static Method sPutObject;
    private static Method sPutBoolean;
    private static Method sPutInt;
    private static Method sPutLong;
    private static Method sPutFloat;
    private static Method sPutDouble;

    private FinalFieldUtils() {
    }

    /**
     * 写入静态字段，字段带 final 时自动绕过 ART 的限制。
     *
     * @return 是否写入成功
     */
    public static boolean setStaticField(@Nullable Class<?> clazz, String fieldName, Object value) {
        Field field = findStaticField(clazz, fieldName);
        if (field == null) {
            XposedLog.e(TAG, "no static field " + fieldName + " in " + clazz);
            return false;
        }
        if (!Modifier.isFinal(field.getModifiers())) {
            try {
                field.setAccessible(true);
                field.set(null, value);
                return true;
            } catch (Throwable e) {
                XposedLog.e(TAG, "failed to set " + field + ": " + e);
                return false;
            }
        }
        return setFinalStaticField(field, value);
    }

    /**
     * 目标字段带 final 时用 Unsafe 写入，否则返回 false 让调用方走原本的反射路径。
     */
    public static boolean trySetFinalStaticField(@Nullable Class<?> clazz, String fieldName, Object value) {
        Field field = findStaticField(clazz, fieldName);
        if (field == null || !Modifier.isFinal(field.getModifiers())) return false;
        return setFinalStaticField(field, value);
    }

    private static boolean setFinalStaticField(Field field, Object value) {
        if (!prepare()) return false;
        try {
            long offset = sOffsetField.getInt(field) & 0xFFFFFFFFL;
            Class<?> base = field.getDeclaringClass();
            Class<?> type = field.getType();
            if (type == boolean.class) {
                sPutBoolean.invoke(sUnsafe, base, offset, value);
            } else if (type == int.class || type == short.class || type == byte.class || type == char.class) {
                sPutInt.invoke(sUnsafe, base, offset, ((Number) value).intValue());
            } else if (type == long.class) {
                sPutLong.invoke(sUnsafe, base, offset, ((Number) value).longValue());
            } else if (type == float.class) {
                sPutFloat.invoke(sUnsafe, base, offset, ((Number) value).floatValue());
            } else if (type == double.class) {
                sPutDouble.invoke(sUnsafe, base, offset, ((Number) value).doubleValue());
            } else {
                sPutObject.invoke(sUnsafe, base, offset, value);
            }
            return true;
        } catch (Throwable e) {
            XposedLog.e(TAG, "failed to set final field " + field + ": " + e);
            return false;
        }
    }

    @Nullable
    private static Field findStaticField(@Nullable Class<?> clazz, String fieldName) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                return Modifier.isStatic(field.getModifiers()) ? field : null;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static synchronized boolean prepare() {
        if (sPrepared) return sUnsafe != null;
        sPrepared = true;
        try {
            // Field.offset 是隐藏字段，反射前先解除限制
            HiddenApiBypass.addHiddenApiExemptions("L");
        } catch (Throwable e) {
            XposedLog.e(TAG, "failed to add hidden api exemptions: " + e);
        }
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Field offsetField = Field.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
            sPutObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
            sPutBoolean = unsafeClass.getMethod("putBoolean", Object.class, long.class, boolean.class);
            sPutInt = unsafeClass.getMethod("putInt", Object.class, long.class, int.class);
            sPutLong = unsafeClass.getMethod("putLong", Object.class, long.class, long.class);
            sPutFloat = unsafeClass.getMethod("putFloat", Object.class, long.class, float.class);
            sPutDouble = unsafeClass.getMethod("putDouble", Object.class, long.class, double.class);
            sOffsetField = offsetField;
            sUnsafe = theUnsafe.get(null);
        } catch (Throwable e) {
            XposedLog.e(TAG, "unsafe writer is unavailable: " + e);
        }
        return sUnsafe != null;
    }
}
