-- ============================================
-- V12: Категории услуг (электрик, репетитор и т.д.)
-- ============================================

-- 5. Услуги
INSERT INTO categories (id, name, parent_id) VALUES (17, 'Услуги', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (18, 'Ремонт и строительство', 17);
INSERT INTO categories (id, name, parent_id) VALUES (19, 'Электрики', 17);
INSERT INTO categories (id, name, parent_id) VALUES (20, 'Сантехники', 17);
INSERT INTO categories (id, name, parent_id) VALUES (21, 'Репетиторы', 17);
INSERT INTO categories (id, name, parent_id) VALUES (22, 'Красота и здоровье', 17);
INSERT INTO categories (id, name, parent_id) VALUES (23, 'Уборка и клининг', 17);
INSERT INTO categories (id, name, parent_id) VALUES (24, 'Грузоперевозки', 17);
INSERT INTO categories (id, name, parent_id) VALUES (25, 'IT и компьютерная помощь', 17);

-- 6. Работа
INSERT INTO categories (id, name, parent_id) VALUES (26, 'Работа', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (27, 'Вакансии', 26);
INSERT INTO categories (id, name, parent_id) VALUES (28, 'Резюме', 26);
INSERT INTO categories (id, name, parent_id) VALUES (29, 'Подработка', 26);

-- 7. Одежда и аксессуары
INSERT INTO categories (id, name, parent_id) VALUES (30, 'Одежда и аксессуары', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (31, 'Мужская одежда', 30);
INSERT INTO categories (id, name, parent_id) VALUES (32, 'Женская одежда', 30);
INSERT INTO categories (id, name, parent_id) VALUES (33, 'Обувь', 30);
INSERT INTO categories (id, name, parent_id) VALUES (34, 'Аксессуары', 30);

-- 8. Хобби и отдых
INSERT INTO categories (id, name, parent_id) VALUES (35, 'Хобби и отдых', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (36, 'Спорт и фитнес', 35);
INSERT INTO categories (id, name, parent_id) VALUES (37, 'Книги и журналы', 35);
INSERT INTO categories (id, name, parent_id) VALUES (38, 'Музыкальные инструменты', 35);
INSERT INTO categories (id, name, parent_id) VALUES (39, 'Игры и приставки', 35);

-- 9. Животные
INSERT INTO categories (id, name, parent_id) VALUES (40, 'Животные', NULL);
INSERT INTO categories (id, name, parent_id) VALUES (41, 'Собаки', 40);
INSERT INTO categories (id, name, parent_id) VALUES (42, 'Кошки', 40);
INSERT INTO categories (id, name, parent_id) VALUES (43, 'Другие животные', 40);

-- Обновляем sequence
SELECT setval('categories_id_seq', 43);
