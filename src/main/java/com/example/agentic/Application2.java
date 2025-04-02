
/* 
* Copyright 2024 - 2024 the original author or authors.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* https://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example.agentic;

import java.util.List;

import com.example.agentic.orchestration.Orchestrator;
import com.example.agentic.orchestration.Types.PlanResult;
import io.modelcontextprotocol.client.McpSyncClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// ------------------------------------------------------------
// ORCHESTRATOR WORKERS
// ------------------------------------------------------------

// @SpringBootApplication
public class Application2 {

	public static void main(String[] args) {
		SpringApplication.run(Application2.class, args);
	}

	public static class FilteredMcpToolCallbackProvider extends SyncMcpToolCallbackProvider {

		public FilteredMcpToolCallbackProvider(List<String> serverNames, List<McpSyncClient> mcpClients) {
			super(filterMcpClients(mcpClients, serverNames));
		}

		public FilteredMcpToolCallbackProvider(List<String> serverNames, McpSyncClient... mcpClients) {
			super(filterMcpClients(List.of(mcpClients), serverNames));
		}

		private static List<McpSyncClient> filterMcpClients(List<McpSyncClient> mcpClients, List<String> serverNames) {
			return mcpClients.stream()
					.filter(mcpClient -> {
						var mcpServerName = mcpClient.getServerInfo().name();
						return serverNames.stream()
								.anyMatch(serverName -> mcpServerName.contains(serverName));
					})
					.toList();
		}

	}

	@Bean
	public CommandLineRunner commandLineRunner(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {

		return args -> {

			var searchAgent = new McpAgent("searcher",
					"""
							You are an expert web researcher. Your role is to:
								1. Search for relevant, authoritative sources on the given topic
								2. Visit the most promising URLs to gather detailed information
								3. Return a structured summary of your findings with source URLs

								Focus on high-quality sources like academic papers, respected tech publications,
								and official documentation.

								Save each individual source in the '/Users/christiantzolov/temp' source folder. 
								The source file content can not be empyt! 
								Use "Empty Source Content" string as a content if there is no other content.
								We only need up to 10 sources max.
								""",
					List.of("brave", "fetch", "filesystem"),
					chatClientBuilder.clone()
							.defaultTools(new FilteredMcpToolCallbackProvider(List.of("brave", "fetch",
									"filesystem"),
									mcpClients))
							.build());

			var factChecker = new McpAgent("fact_checker",
					"""
							You are a meticulous fact checker. Your role is to:
										1. Verify claims by cross-referencing sources
										2. Check dates, statistics, and technical details for accuracy
										3. Identify any contradictions or inconsistencies
									""",
					List.of("filesystem"),
					chatClientBuilder.clone()
							.defaultTools(new FilteredMcpToolCallbackProvider(List.of("filesystem"),
									mcpClients))
							.build());
			// List.of("fetch", "filesystem"),
			// chatClientBuilder.clone()
			// .defaultTools(new FilteredMcpToolCallbackProvider(List.of("fetch",
			// "filesystem"),
			// mcpClients))
			// .build());

			var reportWriter = new McpAgent("writer",
					"""
							You are a technical report writer specializing in research
							         documents. Your role is to:
							         1. Create well-structured, professional reports
							         2. Include proper citations and references
							         3. Balance technical depth with clarity

							         Save your report to the filesystem in the /Users/christiantzolov/temp output folder with appropriate formatting.
									 The saved report file content MUST not be empyt!  Use "Empty Content" as a content if there is no other content.

									""",
					List.of("filesystem"),
					chatClientBuilder.clone()
							.defaultTools(new FilteredMcpToolCallbackProvider(List.of("filesystem"),
									mcpClients))
							.build());
					// List.of("filesystem", "fetch"),
					// chatClientBuilder.clone()
					// 		.defaultTools(new FilteredMcpToolCallbackProvider(List.of("filesystem", "fetch"),
					// 				mcpClients))
					// 		.build());

			var orchestrator = new Orchestrator(
					chatClientBuilder.clone(),
					List.of(searchAgent, reportWriter));
			// List.of(searchAgent, factChecker, reportWriter));

			
			// PlanResult response = orchestrator.execute("Write a shot report on the latest advancements in AI",
			// 		new Orchestrator.RequestParams(3, 16384));
			PlanResult response = orchestrator.execute("Write a summary about Spring AI and MCP Java SDK advancements",
					new Orchestrator.RequestParams(3, 16384));

			System.out.println("Plan: " + response);

		};
	}
}
