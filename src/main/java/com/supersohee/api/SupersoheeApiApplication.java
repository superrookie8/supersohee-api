package com.supersohee.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class SupersoheeApiApplication {

	public static void main(String[] args) {
		// .env 파일 로드
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// 디버깅: 로드된 환경 변수 확인
		System.out.println("=== 환경 변수 로드 확인 ===");
		System.out.println(".env 파일에서 로드된 환경 변수 개수: " + dotenv.entries().size());

		// 중요한 환경 변수 확인
		String[] importantVars = {
				"ADMIN_PASSWORD", "R2_ENDPOINT", "R2_ACCESS_KEY_ID",
				"R2_SECRET_ACCESS_KEY", "MONGO_URI", "JWT_SECRET"
		};

		for (String var : importantVars) {
			String value = dotenv.get(var);
			if (value != null && !value.isEmpty()) {
				System.out.println("✓ " + var + ": 로드됨 (길이: " + value.length() + ")");
			} else {
				System.out.println("✗ " + var + ": 없음 또는 비어있음");
			}
		}
		System.out.println("========================\n");

		// 환경 변수를 System properties로 설정
		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		SpringApplication.run(SupersoheeApiApplication.class, args);
	}

}
