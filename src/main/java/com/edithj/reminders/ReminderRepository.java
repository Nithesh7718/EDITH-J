package com.edithj.reminders;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository {

    List<Reminder> findAll();

    Optional<Reminder> findById(String reminderId);

    Reminder save(Reminder reminder);

    boolean deleteById(String reminderId);
}
