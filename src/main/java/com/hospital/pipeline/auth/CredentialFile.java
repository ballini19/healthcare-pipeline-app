package com.hospital.pipeline.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads password.txt: lines of "username | password | role | name"; everything else is ignored. */
public final class CredentialFile {

    public record Entry(String username, String password, Role role, String name) {
    }

    private CredentialFile() {
    }

    public static List<Entry> load(Path file) throws IOException {
        List<Entry> out = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String[] p = line.split("\\|");
            if (p.length < 4) continue;
            try {
                out.add(new Entry(p[0].trim().toLowerCase(), p[1].trim(),
                        Role.valueOf(p[2].trim().toUpperCase()), p[3].trim()));
            } catch (IllegalArgumentException headerRow) {
                // the "username | password | role | name" heading
            }
        }
        return out;
    }
}
