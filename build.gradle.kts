plugins {
	java
	id("org.springframework.boot") version "3.5.9"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.supersohee"
version = "0.0.1-SNAPSHOT"
description = "Supersohee backend API server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("io.github.cdimascio:dotenv-java:3.0.0")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // OAuth2 Client
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    
    // JWT (토큰 기반 인증용)
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")


    // AWS SDK BOM (버전 관리)
    implementation(platform("software.amazon.awssdk:bom:2.20.0"))

	// AWS SDK for S3 (Cloudflare R2 호환)
    implementation("software.amazon.awssdk:s3:2.20.0")
    implementation("software.amazon.awssdk:auth:2.20.0")
    implementation("software.amazon.awssdk:url-connection-client:2.20.0")
	implementation("software.amazon.awssdk:regions:2.20.0") // Region 사용을 위해 추가
}

tasks.withType<Test> {
	useJUnitPlatform()
}
