package me.stevetech.fullbackup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip {
    public static Object[] ZipFiles(Path backupFolder) {
        String date = LocalDate.now().toString();
        int dayVersion = 0;
        for (File file : backupFolder.toFile().listFiles()) {
            if (file.getName().startsWith(date))
                dayVersion++;
        }

        String backupName = date + '-' + dayVersion + ".zip";

        try {
            pack("", backupFolder.toString() + '/' + backupName, backupFolder, false);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            return new Object[]{backupName, false};
        }
        return new Object[]{backupName, true};
    }

    public static void pack(String sourceDirPath, String zipFilePath, Path ignorePath, Boolean verbose) throws IOException, InterruptedException {
        Path p = Files.createFile(Paths.get(zipFilePath));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            Path pp = Paths.get(sourceDirPath);
            try {
                Files.walk(pp)
                        .filter(path -> !Files.isDirectory(path))
                        .filter(path -> !path.startsWith(ignorePath))
                        .forEach(path -> {
                            if (Thread.interrupted()) throw new RuntimeException();
                            if (verbose) System.out.println(path);
                            ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                if (verbose) System.err.println(e);
                            }
                        });
            } catch (RuntimeException e) {
                throw new InterruptedException();
            }
        }
    }
}
