<div align="center">

# 🌊 Wave 1.5 Reworked

**Minecraft спам-бот нового поколения**

[![Java](https://img.shields.io/badge/Java-11+-orange?style=flat-square&logo=openjdk)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Windows-lightgrey?style=flat-square&logo=windows)]()
[![Telegram](https://img.shields.io/badge/Поддержка-t.me%2Fnnsquado-2CA5E0?style=flat-square&logo=telegram)](https://t.me/nnsquado)

*Автор: **FATZE** · Создано при поддержке [t.me/nnsquado](https://t.me/nnsquado)*

</div>

---

## ✨ Возможности

| Функция | Описание |
|---|---|
| 🌐 **Мульти-сервер** | Боты подключаются к нескольким серверам одновременно |
| 🔍 **Автопарсинг** | Каждые 2 минуты парсит свежие серверы с 5 мониторингов |
| 🔄 **Ротация прокси** | SOCKS4 / SOCKS5 / HTTP — автозагрузка или локальные файлы |
| 🛡️ **Анти-бот фильтр** | Определяет и удаляет серверы с защитой от ботов |
| 💬 **Кастомные команды** | Список команд с настраиваемой задержкой |
| ♻️ **Двойное подключение** | Переподключение при дисконнекте |
| 🚶 **Движение** | Боты двигаются, чтобы не кикало по AFK |
| 🔐 **Решение капч** | Базовое определение и решение капчи |
| 🎨 **Цветная консоль** | ANSI вывод с временными метками |

---

## 🚀 Быстрый старт

### 1. Скачай релиз

Перейди в [Releases](../../releases) и скачай последний `Wave.jar`

### 2. Настрой конфиг

```bash
# Скопируй шаблон
copy config.example.yml config.yml
```

Открой `config.yml` и настрой под себя.

### 3. Запусти

Запусти `Старт.bat` — при первом запуске конфиг создаётся автоматически.

---

## ⚙️ Конфигурация

```yaml
infoFormat: 1          # 0 = подробный вывод, 1 = только статистика

# Боты
botsCount: 10          # ботов за одну волну
joinDelay: 1000        # задержка между волнами (мс)

# Ники
randomNicks: true
randomNicksLength: 8

# Пароли (/register, /login)
randomPasswords: false
randomPasswordsLength: 6

# Поведение
doubleJoin: true       # переподключение при дисконнекте
antiBotFilter: true    # удалять серверы с защитой от ботов
move: false            # движение ботов

# Тестовый режим
testMode: false
testModeIp: "127.0.0.1:25565"

# Авторестарт
autoRestart: false
autoRestartDelay: 60   # минут

# Команды
commands:
  - "/say Привет!"
  - "/help"

commandDelay: 1000     # задержка между командами (мс)
commandLoopDelay: 5000 # задержка между циклами (мс)
```

<details>
<summary>📁 Прокси и ники</summary>

**Прокси** — положи прокси в папку `Proxy/` в нужный файл (по одному `ip:port` на строку):
- `Proxy/socks4.txt` — SOCKS4
- `Proxy/socks5.txt` — SOCKS5
- `Proxy/http.txt` — HTTP

Можно использовать несколько файлов одновременно. Если ни один не найден — все три типа скачиваются автоматически.

**Ники** — положи ники в `nicks.txt` в корне папки (по одному на строку) и поставь `randomNicks: false`.  
Если файл пустой или не существует — автоматически генерируется 100 000 случайных ников и сохраняется в `nicks.txt`.

</details>

---

## 🔍 Источники серверов

Серверы парсятся с шести мониторингов:

| Сайт | Статус |
|---|---|
| [minecraftrating.ru](https://minecraftrating.ru/new-servers/) | ✅ Работает |
| [tmonitoring.com](https://tmonitoring.com/servers-new/) | ✅ Работает |
| [gamemonitoring.ru](https://gamemonitoring.ru/minecraft/servers/new/version/minecraft) | ✅ Работает |
| [minecraft.menu](https://minecraft.menu/minecraft-russia-servers) | ✅ Работает |
| [topminecraftservers.org](https://topminecraftservers.org/) | ✅ Работает |
| [minecraft-server-list.com](https://minecraft-server-list.com/) | ✅ Работает |

Каждый сервер проверяется через [mcsrvstat.us API](https://api.mcsrvstat.us) — остаются только **Paper / Purpur / Spigot** серверы. BungeeCord и Velocity отфильтровываются.

---

## 🛠️ Сборка из исходников

Нужны **Maven 3.6+** и **JDK 11+**

```bash
git clone https://github.com/JustNanix/Wave-Multibot.git
cd Wave-Multibot
mvn clean package
```

Готовый JAR: `target/Wave-1.0-jar-with-dependencies.jar`

---

## 📋 Требования

- ☕ Java 11+
- 🪟 Windows 10+ (ANSI цвета, cmd заголовок)
- 🧠 4GB+ RAM рекомендуется

---

## 📄 Лицензия

Распространяется под лицензией [MIT](LICENSE).

---

<div align="center">

Сделано с ❤️ **FATZE**

[![Telegram](https://img.shields.io/badge/Создано_при_поддержке-t.me%2Fnnsquado-2CA5E0?style=for-the-badge&logo=telegram)](https://t.me/nnsquado)

</div>
