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

/**
 * @author Christian Tzolov
 */

public class OrchestratorPrompts {

	public static final String TASK_RESULT_TEMPLATE = """
			Task: %s
			Result: %s""";

	public static final String STEP_RESULT_TEMPLATE = """
			Step: %s
			Step Subtasks:
			%s""";

	public static final String PLAN_RESULT_TEMPLATE = """
			Plan Objective: %s

			Progress So Far (steps completed):
			%s

			Plan Current Status: %s
			Plan Current Result: %s""";

	public static final String PLAN_PROMPT_TEMPLATE = """
			You are tasked with orchestrating a plan to complete an objective.
			You can analyze results from the previous steps already executed to decide if the objective is complete.
			Your plan must be structured in sequential steps (up to 3 steps), with each step containing independent parallel subtasks (up to 4 subtasks).

			Objective: %s

			%s

			If the previous results achieve the objective, return is_complete=True.
			Otherwise, generate remaining steps needed.

			You have access to the following MCP Servers (which are collections of tools/functions),
			and Agents (which are collections of servers):

			Agents:
			%s

			Generate a plan with all remaining steps needed.
			Steps are sequential, but each Step can have parallel subtasks.
			For each Step, specify a description of the step and independent subtasks that can run in parallel.
			For each subtask specify:
				1. Clear description of the task that an LLM can execute
				2. Name of 1 Agent OR List of MCP server names to use for the task

			Return your response in the following JSON structure:
				\\{
					"steps": [
						\\{
							"description": "Description of step 1",
							"tasks": [
								\\{
									"description": "Description of task 1",
									"agent": "agent_name"  # For AgentTask
								\\},
								\\{
									"description": "Description of task 2",
									"agent": "agent_name2"
								\\}
							]
						\\}
					],
					"is_complete": false
				\\}

			You must respond with valid JSON only, with no triple backticks. No markdown formatting.
			No extra text. Do not wrap in ```json code fences.""";

	public static final String TASK_PROMPT_TEMPLATE = """
			You are part of a larger workflow to achieve the objective: %s.
			Your job is to accomplish only the following task: %s.

			Results so far that may provide helpful context:
			%s
			""";

	public static final String SYNTHESIZE_STEP_PROMPT_TEMPLATE = """
			Synthesize the results of these parallel tasks into a cohesive result:
			%s""";

	public static final String SYNTHESIZE_PLAN_PROMPT_TEMPLATE = """
			Synthesize the results of executing all steps in the plan into a cohesive result:
			%s""";

	// Helper methods to format the templates
	public static String formatTaskResult(String taskDescription, String taskResult) {
		return String.format(TASK_RESULT_TEMPLATE, taskDescription, taskResult);
	}

	public static String formatStepResult(String stepDescription, String tasksStr) {
		return String.format(STEP_RESULT_TEMPLATE, stepDescription, tasksStr);
	}

	public static String formatPlanResult(String planObjective, String stepsStr, String planStatus, String planResult) {
		return String.format(PLAN_RESULT_TEMPLATE, planObjective, stepsStr, planStatus, planResult);
	}

	public static String formatFullPlanPrompt(String objective, String planResult, String agents) {
		return String.format(PLAN_PROMPT_TEMPLATE, objective, planResult, agents);
	}

	public static String formatTaskPrompt(String objective, String task, String context) {
		return String.format(TASK_PROMPT_TEMPLATE, objective, task, context);
	}

	public static String formatSynthesizeStepPrompt(String stepResult) {
		return String.format(SYNTHESIZE_STEP_PROMPT_TEMPLATE, stepResult);
	}

	public static String formatSynthesizePlanPrompt(String planResult) {
		return String.format(SYNTHESIZE_PLAN_PROMPT_TEMPLATE, planResult);
	}

}
