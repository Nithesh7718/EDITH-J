package com.edithj.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.edithj.chat.ConversationHistoryService;
import com.edithj.commands.CalendarCommandHandler;
import com.edithj.commands.CommandHandler;
import com.edithj.commands.DesktopAutomationCommandHandler;
import com.edithj.commands.DesktopToolsCommandHandler;
import com.edithj.commands.EmailCommandHandler;
import com.edithj.commands.FallbackChatHandler;
import com.edithj.commands.LauncherCommandHandler;
import com.edithj.commands.NotesCommandHandler;
import com.edithj.commands.ReminderCommandHandler;
import com.edithj.commands.UtilitiesCommandHandler;
import com.edithj.commands.WeatherCommandHandler;
import com.edithj.commands.WhatsAppCommandHandler;
import com.edithj.config.PreferencesService;
import com.edithj.integration.llm.LlmClient;
import com.edithj.integration.llm.PromptBuilder;
import com.edithj.integration.llm.ProviderBackedLlmClient;
import com.edithj.speech.SpeechService;

/**
 * EDITH runtime orchestrator: normalizes input, classifies intent, routes to
 * the correct hub/handler, and records conversational fallback memory.
 */
public class AssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);
    private static final int DEFAULT_MEMORY_WINDOW = 12;
    private static final double MIN_CONFIDENCE_FOR_AUTO_ROUTE = 0.60d;
    private static final Pattern WHATSAPP_FOLLOWUP_SEND_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:(?:send|sen|snd|message|msg|text)\\b.*|(?:just|jusdt)\\s+send\\b.*)");

    private final SpeechService speechService;
    private final IntentClassifier intentClassifier;
    private final KnowledgeRouter knowledgeRouter;
    private final IntentRouter intentRouter;
    private final FallbackChatService fallbackChatService;
    private final ConversationHistoryService conversationHistoryService;
    private final ActionApprovalPolicy actionApprovalPolicy = new ActionApprovalPolicy();
    private final TaskPlanner taskPlanner = new TaskPlanner();
    private final PreferencesService preferencesService = PreferencesService.instance();
    private final AssistantTelemetry telemetry = AssistantTelemetry.instance();
    private String lastVoiceTranscript = "";
    private IntentType lastStructuredIntent = IntentType.FALLBACK_CHAT;
    private PendingApproval pendingApproval;
    private AssistantResponse.TaskPlan activeTaskPlan;

    public AssistantService() {
        this(new ProviderBackedLlmClient(), new PromptBuilder(), new SpeechService(), new IntentRouter(), null, DEFAULT_MEMORY_WINDOW);
    }

    public AssistantService(ConversationHistoryService conversationHistoryService) {
        this(new ProviderBackedLlmClient(), new PromptBuilder(), new SpeechService(), new IntentRouter(), conversationHistoryService, DEFAULT_MEMORY_WINDOW);
    }

    public AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            IntentRouter intentRouter,
            ConversationHistoryService conversationHistoryService,
            int maxTurns) {
        this(llmClient, promptBuilder, speechService, intentRouter,
                new FallbackChatService(llmClient, promptBuilder, maxTurns), conversationHistoryService, maxTurns);
    }

    AssistantService(LlmClient llmClient,
            PromptBuilder promptBuilder,
            SpeechService speechService,
            IntentRouter intentRouter,
            FallbackChatService fallbackChatService,
            ConversationHistoryService conversationHistoryService,
            int maxTurns) {
        Objects.requireNonNull(llmClient, "llmClient");
        Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.speechService = Objects.requireNonNull(speechService, "speechService");
        this.intentRouter = Objects.requireNonNull(intentRouter, "intentRouter");
        this.fallbackChatService = Objects.requireNonNull(fallbackChatService, "fallbackChatService");
        this.conversationHistoryService = conversationHistoryService;
        this.intentClassifier = new IntentClassifier(llmClient);
        this.knowledgeRouter = new KnowledgeRouter(intentRouter, fallbackChatService, llmClient);

        if (this.conversationHistoryService != null) {
            this.fallbackChatService.populateMemory(this.conversationHistoryService.getRecentHistory(maxTurns));
        }

        registerDefaultHandlers();
        restorePendingApproval();
        restoreActivePlan();
    }

    public void configureTypedFallbackProvider(Function<String, String> typedInputProvider) {
        speechService.typedFallbackService().setTypedInputProvider(typedInputProvider);
    }

    public void registerCommandHandler(CommandHandler handler) {
        intentRouter.registerHandler(handler);
    }

    public AssistantResponse handleTypedInput(String inputText) {
        return handleIncomingInput(inputText, "typed");
    }

    public String respondToTypedInput(String inputText) {
        return handleTypedInput(inputText).answer();
    }

    public void startVoiceInput() {
        speechService.startListening();
    }

    public AssistantResponse stopVoiceInputAndHandle() {
        SpeechService.CapturedInput capturedInput = speechService.stopListening();
        lastVoiceTranscript = capturedInput.text();

        String channel = capturedInput.usedTypedFallback() ? "voice-typed-fallback" : "voice";
        return handleIncomingInput(capturedInput.text(), channel);
    }

    public String stopVoiceInputAndRespond() {
        return stopVoiceInputAndHandle().answer();
    }

    public String getLastVoiceTranscript() {
        return lastVoiceTranscript;
    }

    public boolean isVoiceAvailable() {
        return speechService.isAvailable();
    }

    public boolean isListening() {
        return speechService.isListening();
    }

    public void clearConversationState() {
        fallbackChatService.clearConversationMemory();
        pendingApproval = null;
        activeTaskPlan = null;
        lastStructuredIntent = IntentType.FALLBACK_CHAT;
        lastVoiceTranscript = "";
        preferencesService.clearPendingApproval();
        preferencesService.clearActivePlan();
    }

    private AssistantResponse handleIncomingInput(String rawInput, String channel) {
        String normalized = normalize(rawInput);
        if (normalized.isBlank()) {
            logger.debug("Received blank input from channel: {}", channel);
            return new AssistantResponse(IntentType.FALLBACK_CHAT, "", "Please enter a message.", channel);
        }

        AssistantResponse planned = maybeBuildTaskPlan(normalized, channel);
        if (planned != null) {
            return planned;
        }

        AssistantResponse planExecution = maybeHandlePlanFollowUp(normalized, channel);
        if (planExecution != null) {
            return planExecution;
        }

        AssistantResponse approvalFollowUp = maybeHandleApprovalFollowUp(normalized, channel);
        if (approvalFollowUp != null) {
            return approvalFollowUp;
        }

        logger.info("Processing input from channel: {} | Input: {}", channel, normalized.substring(0, Math.min(100, normalized.length())));
        fallbackChatService.recordUserTurn(normalized);
        if (conversationHistoryService != null) {
            conversationHistoryService.appendUserMessage(normalized);
        }
        AssistantResponse response = enrichResponse(routeWithContextRecovery(normalized, channel));
        logger.info("Intent routed to: {} | Response length: {}", response.intentType(), response.answer().length());
        fallbackChatService.recordAssistantTurn(response.answer());
        if (conversationHistoryService != null) {
            conversationHistoryService.appendAssistantResponse(response);
        }

        if (response.intentType() != IntentType.FALLBACK_CHAT) {
            lastStructuredIntent = response.intentType();
        }

        return response;
    }

    private AssistantResponse maybeHandleApprovalFollowUp(String normalizedInput, String channel) {
        String lower = normalizedInput.toLowerCase();
        if (pendingApproval == null) {
            return null;
        }

        if (lower.equals("approve") || lower.equals("approve last action") || lower.equals("yes, approve")) {
            PendingApproval approved = pendingApproval;
            pendingApproval = null;
            preferencesService.clearPendingApproval();
            return enrichResponse(intentRouter.routeAndHandle(approved.routedIntent(), approved.channel()));
        }

        if (lower.equals("cancel") || lower.equals("cancel last action") || lower.equals("no, cancel")) {
            PendingApproval cancelled = pendingApproval;
            pendingApproval = null;
            preferencesService.clearPendingApproval();
            return new AssistantResponse(
                    IntentType.DESKTOP_AUTOMATION,
                    cancelled.routedIntent().normalizedInput(),
                    "Cancelled the pending action. No system changes were made.",
                    channel,
                    "Automation",
                    true,
                    false,
                    cancelled.approvalType(),
                    "Pending action cancelled.",
                    java.util.List.of(),
                    java.util.List.of(),
                    java.util.Map.of("approvalState", "cancelled"));
        }

        return null;
    }

    private AssistantResponse maybeHandlePlanFollowUp(String normalizedInput, String channel) {
        String lower = normalizedInput.toLowerCase();
        if (activeTaskPlan == null) {
            return null;
        }

        if (lower.equals("start this plan") || lower.equals("start plan")) {
            return executeNextPlanStep(normalizedInput, channel, "started");
        }

        if (lower.equals("continue this plan") || lower.equals("continue plan")) {
            return executeNextPlanStep(normalizedInput, channel, "continued");
        }

        if (lower.equals("show plan status")) {
            return new AssistantResponse(
                    IntentType.GENERAL_CHAT,
                    normalizedInput,
                    "Here is the current plan status.",
                    channel,
                    "Planner",
                    true,
                    false,
                    "",
                    "This is the latest tracked state of your active plan.",
                    activeTaskPlan,
                    allPlanStepsDone(activeTaskPlan)
                    ? List.of()
                    : List.of(new AssistantResponse.AssistantAction("continue-plan", "Continue plan", "plan", "continue this plan")),
                    List.of(),
                    Map.of("planner", allPlanStepsDone(activeTaskPlan) ? "complete" : "active"));
        }

        return null;
    }

    private AssistantResponse maybeBuildTaskPlan(String normalizedInput, String channel) {
        TaskPlanner.PlanResult plan = taskPlanner.plan(normalizedInput, channel);
        if (plan == null) {
            return null;
        }
        List<AssistantResponse.TaskPlanStep> steps = plan.steps().stream()
                .map(step -> new AssistantResponse.TaskPlanStep(
                step.id(),
                step.title(),
                step.tool(),
                step.status(),
                step.detail(),
                step.command()))
                .toList();
        AssistantResponse.TaskPlan taskPlan = new AssistantResponse.TaskPlan(plan.goal(), steps);
        activeTaskPlan = taskPlan;
        persistActivePlan();
        return new AssistantResponse(
                IntentType.GENERAL_CHAT,
                normalizedInput,
                plan.answer(),
                channel,
                "Planner",
                true,
                false,
                "",
                "This request spans multiple actions, so I prepared a structured plan first.",
                taskPlan,
                plan.actions(),
                List.of(),
                Map.of("planner", "deterministic"));
    }

    private AssistantResponse enrichResponse(AssistantResponse response) {
        if (response == null) {
            return new AssistantResponse(IntentType.FALLBACK_CHAT, "", "I could not complete that request.", "typed");
        }
        boolean success = inferSuccess(response);
        String explanation = response.explanation().isBlank() ? inferExplanation(response, success) : response.explanation();
        List<AssistantResponse.RecoveryOption> recoveryOptions = response.recoveryOptions().isEmpty()
                ? inferRecoveryOptions(response, success)
                : response.recoveryOptions();
        return new AssistantResponse(
                response.intentType(),
                response.userInput(),
                response.answer(),
                response.channel(),
                response.source(),
                success,
                response.requiresApproval(),
                response.approvalType(),
                explanation,
                response.taskPlan(),
                response.actions(),
                recoveryOptions,
                response.metadata().isEmpty() ? buildMetadata(response) : response.metadata());
    }

    private Map<String, String> buildMetadata(AssistantResponse response) {
        return Map.of(
                "intentType", response.intentType().name(),
                "channel", response.channel(),
                "source", response.source());
    }

    private boolean inferSuccess(AssistantResponse response) {
        if (response.requiresApproval()) {
            return true;
        }
        String lower = response.answer().toLowerCase(Locale.ROOT);
        if (lower.startsWith("error:")
                || lower.startsWith("voice error:")
                || lower.startsWith("i blocked")
                || lower.startsWith("i could not")
                || lower.startsWith("could not")
                || lower.startsWith("file not found")
                || lower.startsWith("source file does not exist")
                || lower.startsWith("target already exists")
                || lower.startsWith("no handler is configured")
                || lower.startsWith("missing api key")
                || lower.contains("request failed with http")
                || lower.contains("rename aborted")
                || lower.contains("move aborted")) {
            return false;
        }
        return response.success();
    }

    private String inferExplanation(AssistantResponse response, boolean success) {
        if (response.requiresApproval()) {
            return response.explanation();
        }
        if (success) {
            return "";
        }
        return switch (response.intentType()) {
            case DESKTOP_AUTOMATION ->
                "The desktop action did not complete. Review the reason below or try one of the safer follow-up options.";
            case APP_LAUNCH ->
                "The app launch did not complete. The app may be unavailable, blocked, or require a web fallback.";
            case ASK_WEB ->
                "Live web help was limited for this request. A narrower query may work better.";
            default ->
                "I couldn't complete that request cleanly. Try a narrower follow-up or a safer alternative.";
        };
    }

    private List<AssistantResponse.RecoveryOption> inferRecoveryOptions(AssistantResponse response, boolean success) {
        if (success || response.requiresApproval()) {
            return List.of();
        }

        String lower = response.answer().toLowerCase(Locale.ROOT);
        List<AssistantResponse.RecoveryOption> options = new ArrayList<>();

        if (response.intentType() == IntentType.DESKTOP_AUTOMATION) {
            if (lower.contains("file not found") || lower.contains("source file does not exist")) {
                options.add(new AssistantResponse.RecoveryOption("Show recent files", "show recent files"));
                options.add(new AssistantResponse.RecoveryOption("Create the file instead", suggestCreatePrompt(response.userInput())));
            }
            if (lower.contains("target already exists")) {
                options.add(new AssistantResponse.RecoveryOption("Pick a different target", "rename the file using a different target path"));
            }
            if (lower.contains("missing api key") || lower.contains("request failed with http") || lower.contains("unable to reach")) {
                options.add(new AssistantResponse.RecoveryOption("Try a simpler draft", "write a shorter draft for the same file"));
                options.add(new AssistantResponse.RecoveryOption("Switch to notes", "save the content idea as a note instead"));
            }
        }

        if (response.intentType() == IntentType.APP_LAUNCH) {
            options.add(new AssistantResponse.RecoveryOption("Open web fallback", "open the web version instead"));
            options.add(new AssistantResponse.RecoveryOption("Try another app name", "try another app name for this request"));
        }

        return options.isEmpty() ? List.of(
                new AssistantResponse.RecoveryOption("Rephrase request", "help me rephrase this request")) : List.copyOf(options);
    }

    private String suggestCreatePrompt(String originalInput) {
        String normalized = normalize(originalInput);
        String path = normalized.replaceFirst("(?i)^file\\s+(?:open|move|rename)\\s+", "").trim();
        if (path.isBlank()) {
            return "file create text notes/new-file.txt with ";
        }
        String firstToken = path.split("\\s+")[0];
        return "file create text " + firstToken + " with ";
    }

    private AssistantResponse routeWithContextRecovery(String normalizedInput, String channel) {
        if (shouldRecoverToWhatsApp(normalizedInput)) {
            String recoveredInput = ensureWhatsAppPrefix(normalizedInput);
            IntentRouter.RoutedIntent recoveredIntent = intentRouter.route(recoveredInput);
            if (recoveredIntent.intentType() == IntentType.WHATSAPP) {
                return intentRouter.routeAndHandle(recoveredIntent, channel);
            }
        }

        IntentRouter.RoutedIntent directIntent = intentRouter.route(normalizedInput);
        if (directIntent.intentType() == IntentType.DESKTOP_AUTOMATION) {
            ActionApprovalPolicy.ApprovalDecision decision = actionApprovalPolicy.evaluate(
                    directIntent.normalizedInput(),
                    directIntent.intentType());
            if (decision.mode() == ActionApprovalPolicy.ExecutionMode.CONFIRM_ONCE) {
                pendingApproval = new PendingApproval(directIntent, channel, decision.approvalType());
                preferencesService.savePendingApproval(
                        directIntent.normalizedInput(),
                        channel,
                        decision.approvalType());
                return new AssistantResponse(
                        IntentType.DESKTOP_AUTOMATION,
                        directIntent.normalizedInput(),
                        "Approval required before I run that action.",
                        channel,
                        "Automation",
                        true,
                        true,
                        decision.approvalType(),
                        decision.explanation(),
                        decision.actions(),
                        decision.recoveryOptions(),
                        java.util.Map.of("approvalState", "pending"));
            }
            if (decision.mode() == ActionApprovalPolicy.ExecutionMode.BLOCKED) {
                pendingApproval = null;
                preferencesService.clearPendingApproval();
                return new AssistantResponse(
                        IntentType.DESKTOP_AUTOMATION,
                        directIntent.normalizedInput(),
                        "I blocked that action.",
                        channel,
                        "Automation",
                        false,
                        false,
                        decision.approvalType(),
                        decision.explanation(),
                        decision.actions(),
                        decision.recoveryOptions(),
                        java.util.Map.of("approvalState", "blocked"));
            }
            return intentRouter.routeAndHandle(directIntent, channel);
        }

        IntentClassifier.Classification classification = intentClassifier.classify(normalizedInput);
        if (shouldAskForClarification(classification)) {
            return buildClarificationResponse(classification, channel);
        }
        AssistantResponse routedResponse = knowledgeRouter.route(classification, channel);
        if (routedResponse.intentType() != IntentType.FALLBACK_CHAT
                && routedResponse.intentType() != IntentType.GENERAL_CHAT) {
            return routedResponse;
        }

        IntentRouter.RoutedIntent routedIntent = intentRouter.route(normalizedInput);

        if (routedIntent.intentType() != IntentType.FALLBACK_CHAT) {
            return intentRouter.routeAndHandle(routedIntent, channel);
        }

        return routedResponse;
    }

    private boolean shouldAskForClarification(IntentClassifier.Classification classification) {
        if (classification == null) {
            return false;
        }
        return classification.ambiguous()
                && classification.confidenceScore() < MIN_CONFIDENCE_FOR_AUTO_ROUTE
                && !classification.llmRefined();
    }

    private AssistantResponse buildClarificationResponse(IntentClassifier.Classification classification, String channel) {
        telemetry.recordClarificationPrompt();
        String options = classification.candidates().stream()
                .limit(3)
                .map(this::toFriendlyIntentLabel)
                .collect(Collectors.joining(", "));

        String answer = options.isBlank()
                ? "I want to route this correctly. Could you rephrase that request with your main goal?"
                : "I want to route this correctly. Did you mean: " + options
                + "? Please rephrase with one main goal.";

        return new AssistantResponse(
                IntentType.GENERAL_CHAT,
                classification.normalizedInput(),
                answer,
                channel);
    }

    private String toFriendlyIntentLabel(Intent intent) {
        return switch (intent) {
            case OPEN_APP ->
                "open an app";
            case CLOSE_APP ->
                "close an app";
            case DESKTOP_TOOLS ->
                "desktop tools action";
            case ASK_WORLD ->
                "world update";
            case ASK_WORLD_RISK ->
                "country/geopolitical risk";
            case ASK_WORLD_MARKETS ->
                "market snapshot";
            case ASK_LOCAL_KB ->
                "local knowledge lookup";
            case ASK_WEB ->
                "web lookup";
            case GENERAL_CHAT ->
                "general chat";
        };
    }

    private boolean shouldRecoverToWhatsApp(String input) {
        if (lastStructuredIntent != IntentType.WHATSAPP) {
            return false;
        }
        String normalized = normalize(input).toLowerCase();
        if (normalized.isBlank()) {
            return false;
        }
        return WHATSAPP_FOLLOWUP_SEND_PATTERN.matcher(normalized).matches();
    }

    private String ensureWhatsAppPrefix(String input) {
        String normalized = normalize(input);
        if (normalized.toLowerCase().contains("whatsapp")
                || normalized.toLowerCase().contains("whtsapp")
                || normalized.toLowerCase().contains("whatsap")
                || normalized.toLowerCase().contains("watsapp")) {
            return normalized;
        }
        return "whatsapp " + normalized;
    }

    private void registerDefaultHandlers() {
        intentRouter.registerHandler(new NotesCommandHandler());
        intentRouter.registerHandler(new ReminderCommandHandler());
        intentRouter.registerHandler(new LauncherCommandHandler());
        intentRouter.registerHandler(new EmailCommandHandler());
        intentRouter.registerHandler(new CalendarCommandHandler());
        intentRouter.registerHandler(new WhatsAppCommandHandler());
        intentRouter.registerHandler(new WeatherCommandHandler());
        intentRouter.registerHandler(new UtilitiesCommandHandler());
        intentRouter.registerHandler(new DesktopToolsCommandHandler());
        intentRouter.registerHandler(new DesktopAutomationCommandHandler());
        intentRouter.registerHandler(new FallbackChatHandler(context -> fallbackChatService.runFallbackChat(context.channel())));
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }

    private void restorePendingApproval() {
        PreferencesService.PendingApprovalState saved = preferencesService.getPendingApproval();
        if (saved == null || saved.input() == null || saved.input().isBlank()) {
            return;
        }
        IntentRouter.RoutedIntent routedIntent = intentRouter.route(saved.input());
        pendingApproval = new PendingApproval(
                routedIntent,
                saved.channel() == null || saved.channel().isBlank() ? "typed" : saved.channel(),
                saved.approvalType() == null ? "" : saved.approvalType());
    }

    private void restoreActivePlan() {
        PreferencesService.ActivePlanState saved = preferencesService.getActivePlan();
        if (saved == null) {
            return;
        }
        activeTaskPlan = taskPlanner.decodePlan(saved.goal(), saved.encodedPlan());
        if (activeTaskPlan == null) {
            preferencesService.clearActivePlan();
        }
    }

    private void persistActivePlan() {
        if (activeTaskPlan == null) {
            preferencesService.clearActivePlan();
            return;
        }
        preferencesService.saveActivePlan(activeTaskPlan.goal(), taskPlanner.encodePlan(activeTaskPlan));
    }

    private boolean allPlanStepsDone(AssistantResponse.TaskPlan plan) {
        return plan != null
                && !plan.steps().isEmpty()
                && plan.steps().stream().allMatch(step -> "done".equalsIgnoreCase(step.status()));
    }

    private AssistantResponse executeNextPlanStep(String normalizedInput, String channel, String phase) {
        if (activeTaskPlan == null || activeTaskPlan.steps().isEmpty()) {
            return null;
        }

        int stepIndex = nextExecutablePlanStepIndex(activeTaskPlan);
        if (stepIndex < 0) {
            return new AssistantResponse(
                    IntentType.GENERAL_CHAT,
                    normalizedInput,
                    "All tracked steps in this plan are already complete.",
                    channel,
                    "Planner",
                    true,
                    false,
                    "",
                    "There are no remaining plan steps to execute.",
                    activeTaskPlan,
                    List.of(new AssistantResponse.AssistantAction("show-plan-status", "Show final plan", "plan", "show plan status")),
                    List.of(),
                    Map.of("planner", "complete", "planExecution", phase));
        }

        AssistantResponse.TaskPlanStep step = activeTaskPlan.steps().get(stepIndex);
        AssistantResponse toolResponse = enrichResponse(routeWithContextRecovery(step.command().isBlank() ? step.title() : step.command(), channel));
        boolean stepCompleted = didPlanStepComplete(toolResponse);
        String nextStatus = stepCompleted ? "done" : "needs_input";

        List<AssistantResponse.TaskPlanStep> updatedSteps = new ArrayList<>();
        for (int i = 0; i < activeTaskPlan.steps().size(); i++) {
            AssistantResponse.TaskPlanStep current = activeTaskPlan.steps().get(i);
            if (i == stepIndex) {
                updatedSteps.add(new AssistantResponse.TaskPlanStep(
                        current.id(),
                        current.title(),
                        current.tool(),
                        nextStatus,
                        current.detail(),
                        current.command()));
            } else {
                updatedSteps.add(current);
            }
        }

        activeTaskPlan = new AssistantResponse.TaskPlan(activeTaskPlan.goal(), updatedSteps);
        if (allPlanStepsDone(activeTaskPlan)) {
            preferencesService.clearActivePlan();
        } else {
            persistActivePlan();
        }

        String answer = stepCompleted
                ? "Completed step: " + step.title() + ". " + toolResponse.answer()
                : "Attempted step: " + step.title() + ". " + toolResponse.answer();
        String explanation = stepCompleted
                ? (allPlanStepsDone(activeTaskPlan)
                ? "All tracked steps are now complete."
                : "The step ran successfully. Continue the plan when you're ready.")
                : "This step needs more detail before the plan can move forward.";

        return new AssistantResponse(
                IntentType.GENERAL_CHAT,
                normalizedInput,
                answer,
                channel,
                "Planner",
                toolResponse.success(),
                toolResponse.requiresApproval(),
                toolResponse.approvalType(),
                explanation,
                activeTaskPlan,
                mergePlanActions(toolResponse, allPlanStepsDone(activeTaskPlan), stepCompleted),
                toolResponse.recoveryOptions(),
                Map.of(
                        "planner", allPlanStepsDone(activeTaskPlan) ? "complete" : "active",
                        "planExecution", phase,
                        "executedStep", step.id(),
                        "stepStatus", nextStatus));
    }

    private int nextExecutablePlanStepIndex(AssistantResponse.TaskPlan plan) {
        for (int i = 0; i < plan.steps().size(); i++) {
            String status = plan.steps().get(i).status();
            if (!"done".equalsIgnoreCase(status)) {
                return i;
            }
        }
        return -1;
    }

    private boolean didPlanStepComplete(AssistantResponse response) {
        if (response == null || !response.success() || response.requiresApproval()) {
            return false;
        }
        String lower = response.answer().toLowerCase(Locale.ROOT);
        return !(lower.contains("i need")
                || lower.contains("what would you like")
                || lower.contains("didn’t catch")
                || lower.contains("didn't catch")
                || lower.contains("please provide")
                || lower.contains("try:")
                || lower.contains("i can draft")
                || lower.contains("i couldn't parse"));
    }

    private List<AssistantResponse.AssistantAction> mergePlanActions(
            AssistantResponse toolResponse,
            boolean allDone,
            boolean stepCompleted) {
        List<AssistantResponse.AssistantAction> actions = new ArrayList<>(toolResponse.actions());
        if (allDone) {
            actions.add(new AssistantResponse.AssistantAction("show-plan-status", "Show final plan", "plan", "show plan status"));
        } else if (stepCompleted) {
            actions.add(new AssistantResponse.AssistantAction("continue-plan", "Continue plan", "plan", "continue this plan"));
            actions.add(new AssistantResponse.AssistantAction("show-plan-status", "Show plan status", "plan", "show plan status"));
        }
        return List.copyOf(actions);
    }

    private record PendingApproval(IntentRouter.RoutedIntent routedIntent, String channel, String approvalType) {

    }
}
