package com.myblog.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 后端架构依赖护栏测试。
 *
 * @author Codex
 * @since 1.0.0
 */
class BackendArchitectureTest {

    private static final Path MAIN_JAVA = Paths.get("src", "main", "java");

    private static final Set<String> APPLICATION_PERSISTENCE_WHITELIST = new HashSet<String>(Arrays.asList(
        "com/myblog/application/service/AdAppService.java",
        "com/myblog/application/service/AnnouncementAppService.java",
        "com/myblog/application/service/InviteCodeAppService.java",
        "com/myblog/application/service/LearningProgressAppService.java",
        "com/myblog/application/service/SensitiveWordAppService.java",
        "com/myblog/application/service/SensitiveWordCache.java",
        "com/myblog/growth/application/listener/GrantPointOnArticlePublishedListener.java",
        "com/myblog/growth/application/service/AdminPointAppService.java",
        "com/myblog/growth/application/service/InviteRewardAppService.java"
    ));

    private static final Set<String> DOMAIN_INFRASTRUCTURE_WHITELIST = new HashSet<String>(Arrays.asList(
        "com/myblog/domain/repository/LearningProgressRepository.java",
        "com/myblog/growth/domain/service/InviteRiskControlService.java"
    ));

    /**
     * 领域层不能依赖接口层或基础设施层。
     *
     * @throws IOException 文件读取失败时抛出
     */
    @Test
    void domainLayerDoesNotDependOnInterfacesOrInfrastructure() throws IOException {
        List<String> violations = Stream.concat(
                javaFiles(MAIN_JAVA.resolve(Paths.get("com", "myblog", "domain"))),
                javaFiles(MAIN_JAVA.resolve(Paths.get("com", "myblog", "growth", "domain")))
            )
            .filter(path -> !DOMAIN_INFRASTRUCTURE_WHITELIST.contains(relativeMainPath(path)))
            .flatMap(path -> forbiddenImports(path, Arrays.asList(
                "import com.myblog.interfaces.",
                "import com.myblog.infrastructure.",
                "import com.myblog.growth.interfaces.",
                "import com.myblog.growth.infrastructure."
            )).stream())
            .collect(Collectors.toList());

        assertThat(violations).isEmpty();
    }

    /**
     * Application 层不应新增直接 MyBatis 依赖。
     *
     * @throws IOException 文件读取失败时抛出
     */
    @Test
    void applicationLayerDoesNotAddDirectMyBatisDependencies() throws IOException {
        List<String> violations = Stream.concat(
                javaFiles(MAIN_JAVA.resolve(Paths.get("com", "myblog", "application"))),
                javaFiles(MAIN_JAVA.resolve(Paths.get("com", "myblog", "growth", "application")))
            )
            .filter(path -> !APPLICATION_PERSISTENCE_WHITELIST.contains(relativeMainPath(path)))
            .flatMap(path -> forbiddenImports(path, Arrays.asList(
                "import com.myblog.infrastructure.repository.persistence.entity.",
                "import com.myblog.infrastructure.repository.persistence.mapper.",
                "import com.myblog.growth.infrastructure.repository.persistence.entity.",
                "import com.myblog.growth.infrastructure.repository.persistence.mapper."
            )).stream())
            .collect(Collectors.toList());

        assertThat(violations).isEmpty();
    }

    private static Stream<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root)
            .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"));
    }

    private static List<String> forbiddenImports(Path path, List<String> forbiddenPrefixes) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return lines.stream()
                .filter(line -> forbiddenPrefixes.stream().anyMatch(line::startsWith))
                .map(line -> relativeMainPath(path) + ": " + line.trim())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("读取源码失败: " + path, e);
        }
    }

    private static String relativeMainPath(Path path) {
        return MAIN_JAVA.relativize(path).toString().replace('\\', '/');
    }
}
