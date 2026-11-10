package com.foodmap.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台首批管理员受控初始化配置，默认关闭，只能通过显式环境变量启用。
 *
 * <p>密码必须从环境变量或密钥系统注入，不允许写入仓库配置；账号名建议按时间生成，便于区分批次。</p>
 */
@ConfigurationProperties(prefix = "foodmap.admin.bootstrap")
public class AdminBootstrapProperties {
    /**
     * 是否启用受控初始化入口，默认关闭。
     */
    private boolean enabled = false;
    /**
     * 首批管理员条目，账号名为空的条目会被跳过。
     */
    private List<Entry> entries = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * 首批管理员条目配置。
     */
    public static class Entry {
        /**
         * 后台登录账号名。
         */
        private String username;
        /**
         * 后台展示名称。
         */
        private String displayName;
        /**
         * 后台登录密码，只允许从环境变量注入。
         */
        private String password;
        /**
         * 授予的角色编码集合，逗号分隔，为空表示只创建账号不授予角色。
         */
        private List<String> roleCodes = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public void setRoleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes;
        }
    }
}
