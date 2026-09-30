package com.aiops.aiops_apm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiopsApmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiopsApmApplication.class, args);
	}

}
