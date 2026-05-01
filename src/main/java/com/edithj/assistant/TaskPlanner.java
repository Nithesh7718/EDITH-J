package com.edithj.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TaskPlanner {

    public PlanResult plan(String rawInput, String channel) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isBlank()) {
            return null;
        }

        String lower = input.toLowerCase(Locale.ROOT);
        List<TaskStep> steps = new ArrayList<>();

        if (containsReminder(lower)) {
            steps.add(new TaskStep("step-reminder", "Create reminder", "Reminder tool", "pending",
                    "Extract reminder details and save it."));
        }
        if (containsEmail(lower)) {
            steps.add(new TaskStep("step-email", "Draft email", "Email tool", "pending",
                    "Prepare an email draft from the request."));
        }
        if (containsCalendar(lower)) {
            steps.add(new TaskStep("step-calendar", "Open calendar", "Calendar tool", "pending",
                    "Prepare or open the calendar flow."));
        }
        if (containsNotes(lower)) {
            steps.add(new TaskStep("step-notes", "Save note", "Notes tool", "pending",
                    "Capture the important details as a note."));
        }
        if (containsResearch(lower)) {
            steps.add(new TaskStep("step-research", "Research topic", "Web/AI", "pending",
                    "Collect a concise answer or starting points."));
        }

        if (steps.size() < 2) {
            return null;
        }

        String goal = summarizeGoal(input);
        String answer = "I broke this into a task plan so we can execute it step by step.";
        List<AssistantResponse.AssistantAction> actions = List.of(
                new AssistantResponse.AssistantAction("execute-plan", "Execute first step", "plan", "start this plan"),
                new AssistantResponse.AssistantAction("show-plan-status", "Show plan status", "plan", "show plan status"));

        return new PlanResult(goal, steps, answer, channel, actions);
    }

    public ExecutionResult startPlan(AssistantResponse.TaskPlan plan) {
        if (plan == null || plan.steps().isEmpty()) {
            return null;
        }
        List<AssistantResponse.TaskPlanStep> updated = new ArrayList<>();
        boolean startedOne = false;
        for (AssistantResponse.TaskPlanStep step : plan.steps()) {
            if (!startedOne && isPending(step.status())) {
                updated.add(new AssistantResponse.TaskPlanStep(step.id(), step.title(), step.tool(), "in_progress", step.detail()));
                startedOne = true;
            } else {
                updated.add(step);
            }
        }
        if (!startedOne) {
            return new ExecutionResult(plan, "This plan is already in progress or completed.", List.of());
        }
        AssistantResponse.TaskPlan updatedPlan = new AssistantResponse.TaskPlan(plan.goal(), updated);
        return new ExecutionResult(
                updatedPlan,
                "Started the first pending step in the plan.",
                List.of(
                        new AssistantResponse.AssistantAction("continue-plan", "Continue plan", "plan", "continue this plan"),
                        new AssistantResponse.AssistantAction("show-plan-status", "Show plan status", "plan", "show plan status")));
    }

    public ExecutionResult continuePlan(AssistantResponse.TaskPlan plan) {
        if (plan == null || plan.steps().isEmpty()) {
            return null;
        }
        List<AssistantResponse.TaskPlanStep> updated = new ArrayList<>();
        boolean advanced = false;
        boolean markNext = false;

        for (AssistantResponse.TaskPlanStep step : plan.steps()) {
            String status = step.status();
            if (!advanced && "in_progress".equalsIgnoreCase(status)) {
                updated.add(new AssistantResponse.TaskPlanStep(step.id(), step.title(), step.tool(), "done", step.detail()));
                advanced = true;
                markNext = true;
                continue;
            }
            if (markNext && isPending(status)) {
                updated.add(new AssistantResponse.TaskPlanStep(step.id(), step.title(), step.tool(), "in_progress", step.detail()));
                markNext = false;
                continue;
            }
            updated.add(step);
        }

        if (!advanced) {
            return new ExecutionResult(plan, "There isn’t an active step to continue yet. Start the plan first.", List.of(
                    new AssistantResponse.AssistantAction("execute-plan", "Start plan", "plan", "start this plan")));
        }

        AssistantResponse.TaskPlan updatedPlan = new AssistantResponse.TaskPlan(plan.goal(), updated);
        boolean allDone = updated.stream().allMatch(step -> "done".equalsIgnoreCase(step.status()));
        String answer = allDone
                ? "Completed the final tracked step in this plan."
                : "Marked the current step done and advanced the next step.";
        List<AssistantResponse.AssistantAction> actions = allDone
                ? List.of(new AssistantResponse.AssistantAction("show-plan-status", "Show final plan", "plan", "show plan status"))
                : List.of(new AssistantResponse.AssistantAction("continue-plan", "Continue plan", "plan", "continue this plan"));
        return new ExecutionResult(updatedPlan, answer, actions);
    }

    public String encodePlan(AssistantResponse.TaskPlan plan) {
        if (plan == null || plan.steps().isEmpty()) {
            return "";
        }
        return plan.steps().stream()
                .map(step -> escape(step.id()) + "~" + escape(step.title()) + "~" + escape(step.tool()) + "~"
                        + escape(step.status()) + "~" + escape(step.detail()))
                .collect(Collectors.joining("|"));
    }

    public AssistantResponse.TaskPlan decodePlan(String goal, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        List<AssistantResponse.TaskPlanStep> steps = new ArrayList<>();
        for (String rawStep : encoded.split("\\|")) {
            String[] parts = rawStep.split("~", -1);
            if (parts.length < 5) {
                continue;
            }
            steps.add(new AssistantResponse.TaskPlanStep(
                    unescape(parts[0]),
                    unescape(parts[1]),
                    unescape(parts[2]),
                    unescape(parts[3]),
                    unescape(parts[4])));
        }
        if (steps.isEmpty()) {
            return null;
        }
        return new AssistantResponse.TaskPlan(goal == null ? "" : goal, steps);
    }

    private boolean isPending(String status) {
        return status == null || status.isBlank() || "pending".equalsIgnoreCase(status);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\p").replace("~", "\\t");
    }

    private String unescape(String value) {
        return value == null ? "" : value.replace("\\t", "~").replace("\\p", "|").replace("\\\\", "\\");
    }

    private boolean containsReminder(String lower) {
        return lower.contains("reminder") || lower.contains("remind ");
    }

    private boolean containsEmail(String lower) {
        return lower.contains("email") || lower.contains("mail");
    }

    private boolean containsCalendar(String lower) {
        return lower.contains("calendar") || lower.contains("meeting") || lower.contains("event");
    }

    private boolean containsNotes(String lower) {
        return lower.contains("note") || lower.contains("notes") || lower.contains("write down");
    }

    private boolean containsResearch(String lower) {
        return lower.contains("research") || lower.contains("look up") || lower.contains("find out");
    }

    private String summarizeGoal(String input) {
        if (input.length() <= 96) {
            return input;
        }
        return input.substring(0, 93) + "...";
    }

    public record PlanResult(
            String goal,
            List<TaskStep> steps,
            String answer,
            String channel,
            List<AssistantResponse.AssistantAction> actions) {
    }

    public record TaskStep(
            String id,
            String title,
            String tool,
            String status,
            String detail) {
    }

    public record ExecutionResult(
            AssistantResponse.TaskPlan plan,
            String answer,
            List<AssistantResponse.AssistantAction> actions) {
    }
}
