-- Тестовые данные для SLA
INSERT INTO slas (name, response_time_hours, resolution_time_hours, description) VALUES
('Critical', 1, 4, 'Критичные проблемы, требующие немедленного решения'),
('High', 4, 24, 'Высокий приоритет'),
('Medium', 8, 48, 'Средний приоритет'),
('Low', 24, 72, 'Низкий приоритет');

-- Тестовые данные для категорий
INSERT INTO categories (name, description, sla_id) VALUES
('System Outage', 'Полный отказ системы', 1),
('Login Issues', 'Проблемы с авторизацией', 2),
('Performance', 'Проблемы с производительностью', 2),
('Bug Report', 'Сообщения об ошибках', 3),
('Feature Request', 'Запросы новых функций', 4),
('Documentation', 'Вопросы по документации', 4);

-- Тестовые данные для пользователей
INSERT INTO users (name, email) VALUES
('Иван Петров', 'ivan.petrov@example.com'),
('Мария Сидорова', 'maria.sidorova@example.com'),
('Алексей Иванов', 'alexey.ivanov@example.com'),
('Елена Кузнецова', 'elena.kuznetsova@example.com'),
('Дмитрий Смирнов', 'dmitry.smirnov@example.com');

-- Тестовые данные для агентов
INSERT INTO agents (name, email) VALUES
('Анна Волкова', 'anna.volkova@support.com'),
('Сергей Орлов', 'sergey.orlov@support.com'),
('Ольга Морозова', 'olga.morozova@support.com'),
('Павел Зайцев', 'pavel.zaytsev@support.com');

-- Тестовые данные для тикетов
INSERT INTO tickets (title, description, status, user_id, category_id, created_at) VALUES
('Не работает вход в систему', 'При попытке входа выдается ошибка 500', 'OPEN', 1, 2, NOW() - INTERVAL '1 hour'),
('Медленная загрузка страниц', 'Страницы грузятся более 10 секунд', 'IN_PROGRESS', 2, 3, NOW() - INTERVAL '2 hours'),
('Ошибка при сохранении отчета', 'При сохранении отчета появляется сообщение об ошибке', 'RESOLVED', 3, 4, NOW() - INTERVAL '1 day'),
('Добавить темную тему', 'Хотелось бы иметь темную тему интерфейса', 'OPEN', 4, 5, NOW() - INTERVAL '3 hours'),
('Нет документации по API', 'Не могу найти документацию для интеграции', 'CLOSED', 5, 6, NOW() - INTERVAL '5 days');

-- Назначаем агентов на некоторые тикеты
UPDATE tickets SET agent_id = 1 WHERE id = 2;
UPDATE tickets SET agent_id = 2 WHERE id = 3;

-- Устанавливаем дедлайны (на основе SLA категории)
UPDATE tickets t
SET response_deadline = t.created_at + INTERVAL '4 hours',
    resolution_deadline = t.created_at + INTERVAL '24 hours'
FROM categories c
WHERE t.category_id = c.id AND c.sla_id = 2;

-- Закрытый тикет с решением
UPDATE tickets
SET solution = 'Добавлена документация в раздел "Для разработчиков"',
    closed_at = NOW() - INTERVAL '2 days'
WHERE id = 5;