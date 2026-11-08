package jpabook.jpashop;

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JpashopApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpashopApplication.class, args);
	}

	//현재 학습 중인 내용에서는 권장되지 않는 방식이다.
	@Bean
  Hibernate5JakartaModule hibernate5Module() {
		return new Hibernate5JakartaModule();
	}

}
