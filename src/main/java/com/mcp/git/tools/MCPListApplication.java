package com.mcp.git.tools;

import java.util.List;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Needed to expose the tools
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class MCPListApplication {

	private static final Logger logger = LoggerFactory.getLogger(MCPListApplication.class);

	public static void main(String[] args) {
		logger.info("Starting the MCPListApplication");
		SpringApplication.run(MCPListApplication.class, args);
	}

	@Bean
	public List<ToolCallback> tools(GitTools gitTools) {
		logger.info("Registering GitTools with the AI framework");
		return List.of(ToolCallbacks.from(gitTools));
	}

}
