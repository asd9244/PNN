package com.pnn.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication // 스프링 부트 자동 설정 + 컴포넌트 스캔 + 설정 클래스 역할을 한번에 수행
public class BackendApplication {

	public static void main(String[] args) {
		loadDotenvIfPresent();
		SpringApplication.run(BackendApplication.class, args);
		System.out.println("#########Backend Application is running#########");
	}

	/**
	 * Spring Boot는 .env를 읽지 않으므로, 기동 전에 backend/.env(또는 실행 cwd 기준 .env)를
	 * 시스템 프로퍼티로 올린다. 이미 OS 환경 변수로 설정된 값은 덮어쓰지 않는다.
	 */
	private static void loadDotenvIfPresent() {
		Path found = resolveDotenvPath();
		if (found == null || !Files.isRegularFile(found)) {
			return;
		}
		Dotenv dotenv = Dotenv.configure()
				.directory(found.getParent().toString())
				.filename(found.getFileName().toString())
				.ignoreIfMalformed()
				.load();
		dotenv.entries().forEach(e -> {
			String key = e.getKey();
			if (System.getenv(key) == null && System.getProperty(key) == null) {
				System.setProperty(key, e.getValue());
			}
		});
	}

	private static Path resolveDotenvPath() {
		List<Path> candidates = List.of(
				Paths.get(".env"),
				Paths.get("backend", ".env"));
		for (Path relative : candidates) {
			Path abs = relative.toAbsolutePath().normalize();
			if (Files.isRegularFile(abs)) {
				return abs;
			}
		}
		return null;
	}
}
