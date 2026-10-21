package com.scypter.smaintenance.paper.sched;

import org.bukkit.entity.Player;

public interface TaskRunner {
    void runTimer(Runnable r, long periodTicks);

    void runAsync(Runnable r);

    void runMain(Runnable r);

    void runEntity(Player p, Runnable r);

    void runLater(Runnable r, long delayTicks);

    void cancelAll();
}
