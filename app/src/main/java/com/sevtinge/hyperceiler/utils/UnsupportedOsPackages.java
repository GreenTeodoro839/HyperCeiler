package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import java.util.Set;

/**
 * 当前 OS 大版本下已放弃适配的应用。
 * <p>
 * 按模块原则不同时适配多个大版本；在此登记的应用对应分区与设置项将整体隐藏。
 */
public final class UnsupportedOsPackages {

    /**
     * HyperOS 4: 桌面已原生重构 (hasCode=false)，无 Java 代码可 Hook
     */
    private static final Set<String> HYPER_OS4_UNSUPPORTED = Set.of(
        "com.miui.home"
    );

    private UnsupportedOsPackages() {
    }

    public static boolean isUnsupported(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        if (isMoreHyperOSVersion(4f)) {
            return HYPER_OS4_UNSUPPORTED.contains(packageName);
        }
        return false;
    }
}
