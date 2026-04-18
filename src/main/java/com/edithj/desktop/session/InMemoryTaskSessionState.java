package com.edithj.desktop.session;

import java.util.ArrayList;
import java.util.List;

public class InMemoryTaskSessionState implements TaskSessionState {

    private final List<TaskItem> items = new ArrayList<>();

    @Override
    public synchronized TaskItem addTask(String text) {
        int id = items.stream().mapToInt(TaskItem::id).max().orElse(0) + 1;
        TaskItem item = new TaskItem(id, text, false);
        items.add(item);
        return item;
    }

    @Override
    public synchronized List<TaskItem> listTasks() {
        return List.copyOf(items);
    }

    @Override
    public synchronized boolean completeTask(int id) {
        for (int i = 0; i < items.size(); i++) {
            TaskItem item = items.get(i);
            if (item.id() == id) {
                items.set(i, new TaskItem(item.id(), item.text(), true));
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean removeTask(int id) {
        return items.removeIf(item -> item.id() == id);
    }
}
