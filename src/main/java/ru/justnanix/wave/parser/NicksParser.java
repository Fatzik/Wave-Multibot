package ru.justnanix.wave.parser;

import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.Logger;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.StringGenerator;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NicksParser {

    private static final File NICKS_FILE = new File("nicks.txt");

    private final List<String> nicks = new CopyOnWriteArrayList<>();
    private int number = -1;

    public void init() {
        if (Options.randomNicks) return;

        Logger.parserStart("NicksParser");

        try {
            // Создаём файл если не существует
            if (!NICKS_FILE.exists()) {
                NICKS_FILE.createNewFile();
            }

            // Читаем ники из файла (пропускаем пустые строки и комментарии)
            try (BufferedReader reader = new BufferedReader(new FileReader(NICKS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#"))
                        nicks.add(line);
                }
            }

            // Если файл пустой — генерируем и сохраняем
            if (nicks.isEmpty()) {
                Logger.warn("NicksParser  nicks.txt пустой, генерирую ники...");
                generate();
                save();
            }

        } catch (Exception e) {
            Logger.parserError("NicksParser", e.getMessage());
            // Фолбэк — генерируем в памяти без сохранения
            if (nicks.isEmpty()) generate();
        }

        Collections.shuffle(nicks, Wave.getInstance().getRandom());
        Logger.parserDone("NicksParser", nicks.size());
    }

    private void generate() {
        for (int i = 0; i < 100000; i++) {
            // Генерируем буквенно-цифровые ники длиной randomNicksLength
            nicks.add(StringGenerator.generateNick(Options.randomNicksLength));
        }
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NICKS_FILE))) {
            for (String nick : nicks) writer.write(nick + '\n');
        } catch (Exception e) {
            Logger.parserError("NicksParser", "не удалось сохранить nicks.txt: " + e.getMessage());
        }
    }

    public String nextNick() {
        if (nicks.isEmpty()) return "Wave_" + Wave.getInstance().getRandom().nextInt(999999);
        number++;
        if (number >= nicks.size()) number = 0;
        return nicks.get(number);
    }

    public List<String> getNicks() {
        return nicks;
    }
}
