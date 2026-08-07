package com.myproject.website.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 启动前加载本地 {@code .env}。
 * <p>已有非空环境变量不覆盖；空字符串环境变量会被 .env 填上（避免 IDEA 空变量挡住）。
 */
public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {
    }

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            log.warn("未找到 .env（cwd={}），将仅使用环境变量/yml 默认值", Path.of("").toAbsolutePath());
            return;
        }
        int loaded = 0;
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = stripQuotes(line.substring(eq + 1).trim());
                if (hasText(System.getenv(key))) {
                    continue;
                }
                // 系统属性始终可被 .env 补齐空值；非空属性不覆盖
                String existingProp = System.getProperty(key);
                if (hasText(existingProp)) {
                    continue;
                }
                System.setProperty(key, value);
                loaded++;
            }
            log.info("已加载本地环境文件: {}（写入 {} 项）", envFile.toAbsolutePath(), loaded);
        } catch (IOException e) {
            log.warn("读取 .env 失败: {}", envFile.toAbsolutePath());
        }
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static Path resolveEnvFile() {
        Path cwd = Path.of("").toAbsolutePath();
        Path direct = cwd.resolve(".env");
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path inBackend = cwd.resolve("backend").resolve(".env");
        if (Files.isRegularFile(inBackend)) {
            return inBackend;
        }
        // IDEA 有时以模块编译输出目录为 cwd 的上级再上级
        Path fromUserDir = Path.of(System.getProperty("user.dir", "")).resolve(".env");
        if (Files.isRegularFile(fromUserDir)) {
            return fromUserDir;
        }
        Path backendFromUserDir = Path.of(System.getProperty("user.dir", "")).resolve("backend").resolve(".env");
        if (Files.isRegularFile(backendFromUserDir)) {
            return backendFromUserDir;
        }
        return null;
    }
}
