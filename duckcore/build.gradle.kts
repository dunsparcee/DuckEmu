plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.buildtools.native") version "0.11.4"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "io.duckemu"
version = "0.0.1-SNAPSHOT"
description = "Core of duckemu project"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenCentral()
}

extra["springModulithVersion"] = "2.0.3"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("org.springframework.modulith:spring-modulith-starter-jpa")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

graalvmNative {
	binaries {
		named("main") {
			buildArgs.addAll(
				"-J-Xmx7g",
				"-H:DeadlockWatchdogInterval=10", "-H:+DeadlockWatchdogExitOnTimeout"
			)
		}
	}
}
