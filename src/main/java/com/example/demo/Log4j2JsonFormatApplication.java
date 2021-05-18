package com.example.demo;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Log4j2JsonFormatApplication {

	private static Logger logger = LogManager.getLogger(Log4j2JsonFormatApplication.class);
	
	@Autowired
	private  Environment env;

	public static void main(String[] args) throws IOException {
		SpringApplication.run(Log4j2JsonFormatApplication.class, args);
	}

	@PostConstruct
	public void test() throws Exception {
		LogUtils.setLog(env);
		ThreadContext.put("a", "akenarong");
		LogUtils.info("test1");
		logger.error("test2", new Exception("exception my self 2"));
		LogUtils.error("test3", new Exception("exception my self 3"), "");
		LogUtils.error(new Exception("exception my self 4"), "");
		throw new Exception("exception my self 5");
	}

}
