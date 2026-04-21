-- ============================================
-- Seed: Иерархические категории объявлений
-- Минимум 3 корневых × 2+ подкатегорий (по ТЗ)
-- ============================================

-- 1. Транспорт
INSERT INTO categories (id, name, parent_id) VALUES (1, 'Транспорт', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (2, 'Автомобили', 1);
INSERT INTO categories (id, name, parent_id) VALUES (3, 'Мотоциклы и мототехника', 1);
INSERT INTO categories (id, name, parent_id) VALUES (4, 'Запчасти и аксессуары', 1);

-- 2. Недвижимость
INSERT INTO categories (id, name, parent_id) VALUES (5, 'Недвижимость', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (6, 'Квартиры', 5);
INSERT INTO categories (id, name, parent_id) VALUES (7, 'Комнаты', 5);
INSERT INTO categories (id, name, parent_id) VALUES (8, 'Дома и коттеджи', 5);

-- 3. Электроника
INSERT INTO categories (id, name, parent_id) VALUES (9, 'Электроника', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (10, 'Телефоны', 9);
INSERT INTO categories (id, name, parent_id) VALUES (11, 'Ноутбуки', 9);
INSERT INTO categories (id, name, parent_id) VALUES (12, 'Планшеты', 9);

-- 4. Для дома
INSERT INTO categories (id, name, parent_id) VALUES (13, 'Для дома', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (14, 'Мебель', 13);
INSERT INTO categories (id, name, parent_id) VALUES (15, 'Бытовая техника', 13);
INSERT INTO categories (id, name, parent_id) VALUES (16, 'Посуда', 13);

-- Обновляем sequence после ручной вставки ID
SELECT setval('categories_id_seq', 16);
