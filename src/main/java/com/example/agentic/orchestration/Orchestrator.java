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
package com.example.agentic.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.agentic.McpAgent;
import com.example.agentic.orchestration.Types.AgentTask;
import com.example.agentic.orchestration.Types.Plan;
import com.example.agentic.orchestration.Types.Step;
import com.example.agentic.orchestration.Types.TaskWithResult;

import org.springframework.ai.chat.client.ChatClient;

/**
 * @author Christian Tzolov
 */
public class Orchestrator {

	private final ChatClient planner;

	private final Map<String, McpAgent> agents;

	public record RequestParams(int maxIterations, int maxTokens) {

		public RequestParams {
			if (maxIterations < 1) {
				throw new IllegalArgumentException("maxIterations must be greater than 0");
			}
			if (maxTokens < 1) {
				throw new IllegalArgumentException("maxTokens must be greater than 0");
			}
		}

		public RequestParams() {
			this(30, 16384);
		}
	}

	public Orchestrator(ChatClient.Builder plannerBuilder, List<McpAgent> agents) {
		this.planner = plannerBuilder
				.defaultSystem(
						"""
								You are an expert planner. Given an objective task and a list of MCP servers (which are collections of tools)
								or Agents (which are collections of servers), your job is to break down the objective into a series of steps,
								which can be performed by LLMs with access to the servers or agents.
								""")
				.build();
		this.agents = agents.stream()
				.collect(Collectors.toMap(McpAgent::getName, agent -> agent));
	}

	public Types.PlanResult execute(String objective, RequestParams requestParams) {
		var planResult = new Types.PlanResult(objective);

		int iteration = 0;
		while (requestParams.maxIterations >= iteration) {
			iteration++;

			var plan = generatePlan(objective, planResult, requestParams);

			planResult = planResult.withPlan(plan);

			if (plan.isComplete()) {
				planResult = planResult.withCompletion(true);

				String synthesisPrompt = OrchestratorPrompts.SYNTHESIZE_PLAN_PROMPT_TEMPLATE
						.formatted(ResultFormatter.formatPlanResult(planResult));

				var planResultResult = this.planner.prompt()
						.user(synthesisPrompt)
						.call().content();

				planResult = planResult.withFinalResult(planResultResult);

				return planResult;

			}

			// Execute each step, collecting results
			// Note that in iterative mode this will only be a single step

			System.out.println("----------------------------------------------------------------------------");
			System.out.println("> Plan Iteration: " + iteration +  ", isComplete: " + plan.isComplete());
			
			int stepIdx = 1;
			for (var step : plan.steps()) {
				System.out.println("    > Step: %s/%s: %s".formatted(stepIdx++, plan.steps().size(), step.description()));
				
				var setpResult = this.executeStep(step, planResult, requestParams);

				planResult = planResult.withAddedStepResult(setpResult);
			}
		}

		throw new IllegalStateException(
				"Max iterations (%s) reached without completing the plan".formatted(requestParams.maxIterations));
	}

	/**
	 * Generate full plan considering previous results
	 */
	private Types.Plan generatePlan(String objective, Types.PlanResult previousPlanResult, RequestParams requestParams) {

		List<String> formattedAgentsList = new ArrayList<>();
		int idx = 1;
		for (Map.Entry<String, McpAgent> entry : agents.entrySet()) {
			String key = entry.getKey();
			McpAgent agent = entry.getValue();
			formattedAgentsList.add((idx + 1) + ". " + this.formatAgentInfo(agent.getName()));
			System.out.println("\n" + key + " -> " + agent);
			idx++;
		}

		String formatedAgents = String.join("\n", formattedAgentsList);

		var prompt = OrchestratorPrompts.PLAN_PROMPT_TEMPLATE
				.formatted(objective, ResultFormatter.formatPlanResult(previousPlanResult), formatedAgents);

		var plan = this.planner.prompt()
				.user(prompt)
				.call()
				.entity(Plan.class);

		System.out.println("\nPlan: " + plan + "\n");
		
		return plan;
	}

	/**
	 * Execute a step's subtasks in parallel and synthesize results
	 */
	private Types.StepResult executeStep(Step step, Types.PlanResult previousResult, RequestParams requestParams) {

		Types.StepResult stepResult = new Types.StepResult(step);

		// Format previous results
		String context = ResultFormatter.formatPlanResult(previousResult);

		int taskIdx = 1;
		for (AgentTask task : step.tasks()) {
			
			System.out.println("      > Task: %s/%s (%s): %s".formatted(taskIdx++, step.tasks().size(), task.agent(), task.description()));

			McpAgent agent = this.agents.get(task.agent());
			if (agent == null) {
				throw new IllegalStateException("Agent %s not found".formatted(task.agent()));
			}

			String task_description = OrchestratorPrompts.TASK_PROMPT_TEMPLATE.formatted(
					previousResult.objective(),
					task.description(),
					context);
			
			var taskResult = agent.getChatClient().prompt()
					.user(task_description)
					.call()
					.content();
					
			stepResult = stepResult.withAddedTaskResult(new TaskWithResult(task.description(), taskResult));
		}
		
		stepResult = stepResult.withResult(ResultFormatter.formatStepResult(stepResult));

		// System.out.println("Step result: " + stepResult.result());

		return stepResult;			

	}

	/**
	 * Format server information for display to planners
	 */
	private String formatServerInfo(String serverName) {
		String serverStr = "Server Name: " + serverName;
		// ServerConfig serverConfig = this.serverRegistry.getServerConfig(serverName);
		// if (serverConfig == null) {
		// return serverStr;
		// }

		// String description = serverConfig.getDescription();
		// if (description != null && !description.isEmpty()) {
		// serverStr = serverStr + "\nDescription: " + description;
		// }

		return serverStr;
	}

	/**
	 * Format Agent information for display to planners
	 */
	private String formatAgentInfo(String agentName) {
		McpAgent agent = this.agents.get(agentName);
		if (agent == null) {
			return "";
		}

		List<String> formattedServers = new ArrayList<>();
		for (String serverName : agent.getServerNames()) {
			formattedServers.add("- " + this.formatServerInfo(serverName));
		}
		String servers = String.join("\n", formattedServers);

		return "Agent Name: " + agent.getName() +
				"\nDescription: " + agent.getInstruction() +
				"\nServers in Agent: " + servers;
	}
}
