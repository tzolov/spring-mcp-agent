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

import com.example.agentic.orchestration.Types.PlanResult;
import com.example.agentic.orchestration.Types.StepResult;
import com.example.agentic.orchestration.Types.TaskWithResult;

/**
 * @author Christian Tzolov
 */

public class ResultFormatter {

	private static final String TASK_RESULT_TEMPLATE = "Task: %s\nResult: %s";
	private static final String STEP_RESULT_TEMPLATE = "Step: %s\nResult: %s\nTasks:\n%s";
	private static final String PLAN_RESULT_TEMPLATE = "Plan Objective: %s\n\nSteps:\n%s\n\nStatus: %s\nResult: %s";

	/**
	 * Format a task result for display to planners
	 */
	public static String formatTaskResult(TaskWithResult taskResult) {
		return String.format(TASK_RESULT_TEMPLATE,
				taskResult.description(),
				taskResult.result());
	}

	/**
	 * Format a step result for display to planners
	 */
	public static String formatStepResult(StepResult stepResult) {
		StringBuilder tasksStr = new StringBuilder();
		for (TaskWithResult task : stepResult.taskResults()) {
			tasksStr.append("  - ").append(formatTaskResult(task)).append("\n");
		}

		return String.format(STEP_RESULT_TEMPLATE,
				stepResult.step().description(),
				stepResult.result(),
				tasksStr.toString());
	}

	/**
	 * Format the full plan execution state for display to planners
	 */
	public static String formatPlanResult(PlanResult planResult) {
		String stepsStr;
		if (planResult.stepResults() != null && !planResult.stepResults().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < planResult.stepResults().size(); i++) {
				sb.append(i + 1).append(":\n")
						.append(formatStepResult(planResult.stepResults().get(i)));

				if (i < planResult.stepResults().size() - 1) {
					sb.append("\n\n");
				}
			}
			stepsStr = sb.toString();
		} else {
			stepsStr = "No steps executed yet";
		}

		return String.format(PLAN_RESULT_TEMPLATE,
				planResult.objective(),
				stepsStr,
				planResult.isComplete() ? "Complete" : "In Progress",
				planResult.isComplete() ? planResult.result() : "In Progress");
	}
}
