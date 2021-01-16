package me.stevetech.fullbackup;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FullBackup extends JavaPlugin {
    String chatPrefix = ChatColor.DARK_GREEN + "[" + ChatColor.GREEN + getDescription().getName() + ChatColor.DARK_GREEN + "] " + ChatColor.RESET;
    Thread backupTask;
    BukkitTask scheduleTask;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        startSchedule();

        getLogger().info(getDescription().getName() + ' ' + getDescription().getVersion() + " has been Enabled");
    }

    @Override
    public void onDisable() {
        if (backupTask != null && backupTask.isAlive()) {
            getLogger().warning("Stopping Backup...");
            backupTask.interrupt();
            try {
                backupTask.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        saveConfig();

        getLogger().info(getDescription().getName() + ' ' + getDescription().getVersion() + " has been Disabled");
    }

    private void startTask() {
        backupTask = new Thread(() -> {
            Path backupFolder = Paths.get(getConfig().getString("backup-folder"));
            try {
                Files.createDirectories(backupFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!Files.isDirectory(backupFolder)) {
                getServer().broadcast(chatPrefix + ChatColor.DARK_RED + "Backup folder is a file, when it must be a directory.", "FullBackup");
            } else {
                getServer().broadcast(chatPrefix + ChatColor.GREEN + "Starting backup...", "FullBackup");
                Object[] backup = Zip.ZipFiles(backupFolder);
                String backupName = (String) backup[0];
                if ((Boolean) backup[1]) {
                    getServer().broadcast(chatPrefix + ChatColor.GREEN + "Finished Backup: " + backupName, "FullBackup");
                } else {
                    getServer().broadcast(chatPrefix + ChatColor.RED + "Cancelled Backup: " + backupName, "FullBackup");
                }
            }
        });
        backupTask.start();
    }

    private void startSchedule() {
        if (!getConfig().getBoolean("schedule.enabled")) return;
        final List<Integer> minutes = cronToArray(getConfig().getString("schedule.minute"));
        final List<Integer> hour = cronToArray(getConfig().getString("schedule.hour"));
        final List<Integer> day = cronToArray(getConfig().getString("schedule.day"));
        final List<Integer> month = cronToArray(getConfig().getString("schedule.month"));
        final List<Integer> dayOfWeek = cronToArray(getConfig().getString("schedule.day-of-week"));
        scheduleTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            LocalDateTime time = LocalDateTime.now();

            if ((minutes == null || minutes.contains(time.getMinute())) &&
                    (hour == null || hour.contains(time.getHour())) &&
                    (day == null || day.contains(time.getDayOfMonth())) &&
                    (month == null || month.contains(time.getMonthValue())) &&
                    (dayOfWeek == null || dayOfWeek.contains(time.getDayOfWeek().getValue()))) {
                startTask();
            }
        },0L, 60L*20L); // Run every minute
    }

    private List<Integer> cronToArray(String cron) {
        if (cron.equals("*"))
            return null;
        if (StringUtils.isNumeric(cron))
            return Collections.singletonList(Integer.parseInt(cron));

        List<Integer> numbers = new ArrayList<>();
        for (String s: cron.split(",")) {
            if (s.contains("-")) {
                String[] split = s.split("-");
                numbers.addAll(IntStream.rangeClosed(Integer.parseInt(split[0]), Integer.parseInt(split[1]))
                        .boxed().collect(Collectors.toList()));
            } else {
                if (StringUtils.isNumeric(s))
                    numbers.add(Integer.parseInt(s));
            }
        }
        return numbers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("FullBackup") && sender.hasPermission("FullBackup")) {
            if (args.length == 1) {
                switch (args[0]) {
                    case "start":
                        if (!(backupTask != null && backupTask.isAlive())) {
                            startTask();
                        } else {
                            sender.sendMessage(chatPrefix + ChatColor.RED + "Backup is already running.");
                        }
                        return true;
                    case "stop":
                        if (backupTask != null && backupTask.isAlive()) {
                            getServer().broadcast(chatPrefix + ChatColor.RED + "Stopping Backup...", "FullBackup");
                            backupTask.interrupt();
                        } else {
                            sender.sendMessage(chatPrefix + ChatColor.RED + "There is no backup already running.");
                        }
                        return true;
                    case "reload":
                        reloadConfig();
                        scheduleTask.cancel();
                        startSchedule();
                        sender.sendMessage(chatPrefix + ChatColor.YELLOW + "Reloaded Config");
                        return true;
                    default:
                        return false;
                }
            }
        }
        return false;
    }
}
