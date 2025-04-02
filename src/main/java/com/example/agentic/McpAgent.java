/* 
* Copyright 2025 - 2025 the original author or authors.
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

import org.springframework.ai.chat.client.ChatClient;

/**
 * @author Christian Tzolov
 */
public class McpAgent {

	/**
	 * The name of this agent.
	 */
	private String name;
	
	/**
	 * The description of this agent.
	 */
	private String instruction;
	/**
	 * List of MCP server names that this agent can access.
	 */
	private List<String> serverNames;
	/**
	 * The ChatClient instance that this agent uses to communicate with LLM.
	 * This is typically a ChatClient instance that is configured to use the MCP
	 * server's tools for the selected server names.
	 */
	private ChatClient chatClient;

	public McpAgent(String name, String description, List<String> serverNames, ChatClient chatClient) {
		this.name = name;
		this.instruction = description;
		this.serverNames = serverNames;
		this.chatClient = chatClient;
	}

	public String getName() {
		return name;
	}

	public String getInstruction() {
		return instruction;
	}

	public List<String> getServerNames() {
		return serverNames;
	}

	public ChatClient getChatClient() {
		return chatClient;
	}

	@Override
	public String toString() {
		return "McpAgent{" +
				"name='" + name + '\'' +
				", instruction='" + instruction + '\'' +
				", serverNames=" + serverNames +
				'}';
	}
}
