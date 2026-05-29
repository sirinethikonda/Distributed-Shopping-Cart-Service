package com.CartService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedShoppingCartServiceWithRedisCachingApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedShoppingCartServiceWithRedisCachingApplication.class, args);
	}

}
