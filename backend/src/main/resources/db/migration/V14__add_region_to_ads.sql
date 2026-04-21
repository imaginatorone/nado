-- поле региона для фильтрации по местоположению
ALTER TABLE ads ADD COLUMN IF NOT EXISTS region VARCHAR(100);

-- регион пользователя по умолчанию
ALTER TABLE users ADD COLUMN IF NOT EXISTS region VARCHAR(100);
