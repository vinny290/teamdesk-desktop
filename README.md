# TeamDesk Desktop Agent

## Что это

Приложение, которое запускается на компьютере пользователя.

Отвечает за:

- регистрацию машины на сервере
- отправку heartbeat
- подключение к WebSocket signaling
- получение команд от сервера
- показ запроса согласия (consent)

---

## Требования

- Java 17+

---

## Конфиг

Файл:
src/main/resources/agent.properties
`
properties server.http.base-url=http://192.168.1.20:8080 server.ws.url=ws://192.168.1.20:8080/ws/signaling machine.id=desktop-agent-1 heartbeat.interval.seconds=5
`

### ВАЖНО

* НЕ используй `localhost`, если сервер на другом ПК
* Используй IP сервера (например `192.168.1.20`)

---

## Запуск

### Через IntelliJ

Запусти:
AgentApplication
### Через jar
bash mvn clean package java -jar target/desktop-agent-*.jar
---

## Что должно быть в логах
Machine registered WebSocket connected REGISTER_AGENT sent Heartbeat sent
---

## Проверка

Открой на сервере:
http://<server-ip>:8080/api/machines
Должна появиться машина:
json { "machineId": "desktop-agent-1", "status": "ONLINE" }
---

## Как работает

Agent делает:

1. Регистрация:
POST /api/machines/register
2. Heartbeat:
POST /api/machines/heartbeat
3. WebSocket:
ws://server:8080/ws/signaling
4. Отправляет:
json { "type": "REGISTER_AGENT", "machineId": "desktop-agent-1" }
---
