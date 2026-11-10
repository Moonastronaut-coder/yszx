package com.yunshu.zhixue.platform.security;

public enum Realm {
    PORTAL, ADMIN;

    public static Realm fromPath(String path) {
        if (path.startsWith("/api/v1/portal/")) return PORTAL;
        if (path.startsWith("/api/v1/admin/")) return ADMIN;
        return null;
    }
}
