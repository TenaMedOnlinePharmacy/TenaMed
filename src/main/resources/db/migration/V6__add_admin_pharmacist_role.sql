INSERT INTO roles (id, name)
SELECT '8b4bf3f8-9f31-4f4d-a3e9-2b7d6c7c3f2a', 'ADMIN_PHARMACIST'
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE lower(name) = 'admin_pharmacist'
);
