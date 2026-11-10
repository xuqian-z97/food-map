package com.foodmap.admin.service;

import com.foodmap.common.validation.Check;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * 后台管理员 PBKDF2 密码哈希服务，负责把原始密码转换为带盐哈希字符串。
 *
 * <p>pepper 只从配置 {@code foodmap.admin.password.pepper} 注入，本类不提供无条件默认 pepper。
 * 生产环境必须通过环境变量 {@code ADMIN_PEPPER} 显式配置，缺失时启动阶段直接失败；只有
 * {@code local} / {@code orbstack} 这类本机开发 profile 才允许回退到开发 pepper，避免本机启动管理后台
 * 必须额外注入密钥。</p>
 *
 * <p>排查管理员登录失败时，应先确认部署环境是否变更过 {@code ADMIN_PEPPER}：pepper 不落库，
 * 变更后历史哈希将无法通过校验，需要重新生成管理员密码哈希。</p>
 */
@Component
public class Pbkdf2AdminPasswordHashService implements AdminPasswordHashService {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    /**
     * 管理员密码 pepper 配置键，生产环境由环境变量 {@code ADMIN_PEPPER} 注入。
     */
    private static final String PEPPER_KEY = "foodmap.admin.password.pepper";

    /**
     * 管理员密码 pepper 对应的环境变量名，用于启动失败时给出可执行的修复提示。
     */
    private static final String PEPPER_ENV = "ADMIN_PEPPER";

    /**
     * 本机开发 profile 回退 pepper，仅用于 local / orbstack，保持历史本地管理员密码哈希仍可校验。
     */
    private static final String DEV_FALLBACK_PEPPER = "foodmap-admin-local-pepper";

    /**
     * 允许回退到开发 pepper 的 profile 集合，其它 profile 一律要求显式注入。
     */
    private static final Set<String> DEV_PROFILES = Set.of("local", "orbstack");

    private final String pepper;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Spring 注入入口：从配置读取 pepper，并只在 local / orbstack profile 下允许开发回退值。
     *
     * @param pepper 配置项 {@code foodmap.admin.password.pepper} 的绑定值，缺失时为空字符串。
     * @param environment Spring 环境，用于读取当前激活 profile 判断是否允许开发回退值。
     */
    @Autowired
    public Pbkdf2AdminPasswordHashService(
            @Value("${foodmap.admin.password.pepper:}") String pepper,
            Environment environment
    ) {
        this.pepper = resolvePepper(pepper, environment.getActiveProfiles());
    }

    /**
     * 使用显式指定的 pepper 创建服务实例，供单元测试和显式配置场景使用。
     *
     * @param pepper 密码哈希 pepper。
     */
    public Pbkdf2AdminPasswordHashService(String pepper) {
        this.pepper = Check.notBlank("pepper", pepper);
    }

    /**
     * 将后台管理员原始密码转换为可持久化 PBKDF2 哈希。
     *
     * @param rawPassword 原始密码。
     * @return 格式化后的密码哈希。
     */
    @Override
    public String hash(String rawPassword) {
        String checkedPassword = Check.notBlank("rawPassword", rawPassword);
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = hash(checkedPassword, salt);
        return ALGORITHM + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 执行 PBKDF2 哈希计算。
     *
     * @param rawPassword 原始密码。
     * @param salt 随机盐。
     * @return PBKDF2 哈希字节数组。
     */
    private byte[] hash(String rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec((rawPassword + pepper).toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to hash admin password", e);
        }
    }

    /**
     * 解析管理员密码 pepper：显式配置优先，只有 local / orbstack 允许回退到开发值。
     *
     * @param configuredPepper 配置项 {@code foodmap.admin.password.pepper} 的绑定值，可能为空。
     * @param activeProfiles 当前激活的 Spring profile 列表，来自 Spring Environment。
     * @return 可用于哈希的密码 pepper。
     * @throws IllegalStateException 非开发 profile 且未配置 pepper 时抛出，直接阻止服务启动。
     */
    private static String resolvePepper(String configuredPepper, String[] activeProfiles) {
        if (configuredPepper != null && !configuredPepper.isBlank()) {
            return configuredPepper;
        }
        if (hasDevProfile(activeProfiles)) {
            return DEV_FALLBACK_PEPPER;
        }
        String profiles = activeProfiles == null || activeProfiles.length == 0
                ? "<none>"
                : String.join(",", activeProfiles);
        throw new IllegalStateException(PEPPER_KEY + " 未配置：非 local/orbstack 环境必须通过环境变量 "
                + PEPPER_ENV + " 注入管理员密码 pepper，当前激活 profile=" + profiles);
    }

    /**
     * 判断当前激活 profile 是否包含允许使用开发默认值的本机 profile。
     *
     * @param activeProfiles 当前激活的 Spring profile 列表，可能为空。
     * @return 任一激活 profile 属于 local / orbstack 时返回 true，其它情况返回 false。
     */
    private static boolean hasDevProfile(String[] activeProfiles) {
        if (activeProfiles == null) {
            return false;
        }
        for (String profile : activeProfiles) {
            if (profile != null && DEV_PROFILES.contains(profile.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
