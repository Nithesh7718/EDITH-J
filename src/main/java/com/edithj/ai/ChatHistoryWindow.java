package com.edithj.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ChatHistoryWindow {

    public record ChatTurn(String role, String content) {

    }

    private final Deque<ChatTurn> turns;
    private final int maxTurns;

    public ChatHistoryWindow(int maxTurns) {
        this.maxTurns = Math.max(2, maxTurns);
        this.turns = new ArrayDeque<>(this.maxTurns);
    }

    public synchronized void addTurn(String role, String content) {
        if (turns.size() >= maxTurns) {
            turns.removeFirst();
        }
        turns.addLast(new ChatTurn(role == null ? "" : role.trim(), content == null ? "" : content.trim()));
    }

    public synchronized List<ChatTurn> snapshot() {
        return new ArrayList<>(turns);
    }
}
