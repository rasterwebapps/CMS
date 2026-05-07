SELECT 'CREATE DATABASE cmsdb_test'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cmsdb_test')\gexec

