package ru.justnanix.wave.parser;

import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.Logger;
import ru.justnanix.wave.utils.Options;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NicksParser {
    private final List<String> nicks = new CopyOnWriteArrayList<>();
    private int number = -1;

    public void init() {
        if (Options.randomNicks) return;

        Logger.parserStart("NicksParser");

        try {
            File file = new File("Nicks\\nicks.txt");

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    while (reader.ready()) nicks.add(reader.readLine());
                }
                Logger.parserDone("NicksParser", nicks.size());
            } else {
                Logger.warn("NicksParser  nicks.txt не найден, генерирую...");

                for (int i = 0; i < 100000; i++)
                    nicks.add("Wave_" + Wave.getInstance().getRandom().nextInt(1000000));

                new File("Nicks").mkdirs();
                file.createNewFile();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    for (String nick : nicks) writer.write(nick + '\n');
                }

                Logger.parserDone("NicksParser", nicks.size());
            }
        } catch (Exception e) {
            Logger.parserError("NicksParser", e.getMessage());
        }

        Collections.shuffle(nicks, Wave.getInstance().getRandom());
    }

    public String nextNick() {
        number++;

        if (number >= nicks.size())
            number = 0;

        return nicks.get(number);
    }

    public List<String> getNicks() {
        return nicks;
    }
}
