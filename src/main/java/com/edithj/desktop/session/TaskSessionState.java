package com.edithj.desktop.session;

import java.util.List;

public interface TaskSessionState {

    TaskItem addTask(String text);

    List<TaskItem> listTasks();

    boolean completeTask(int id);

    boolean removeTask(int id);
}
