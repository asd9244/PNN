package com.pnn.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 스프링 부트 자동 설정 + 컴포넌트 스캔 + 설정 클래스 역할을 한번에 수행
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args); // 내장 톰캣 서버를 띄우고 스프링 컨텍스트를 초기화 
		System.out.println("########## Backend Application is running ##########");
		System.out.println("########## Backend Application is running ##########");
		System.out.println("########## Backend Application is running ##########");
	}

}
