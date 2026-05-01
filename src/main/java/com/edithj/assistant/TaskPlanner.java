package com.edithj.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                new AssistantResponse.AssistantAction("refine-plan", "Refine plan", "plan", "refine this plan"));

        return new PlanResult(goal, steps, answer, channel, actions);
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
}
