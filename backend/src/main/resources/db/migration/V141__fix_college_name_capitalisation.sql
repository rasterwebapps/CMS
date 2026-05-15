-- Correct capitalisation of the college name stored in system_configurations.
-- Official name: SKS College Of Nursing (capital O in "Of")
-- Previously stored as: SKS College of Nursing
UPDATE system_configurations
SET    config_value = 'SKS College Of Nursing',
       updated_at   = CURRENT_TIMESTAMP
WHERE  config_key   = 'college.name'
  AND  config_value = 'SKS College of Nursing';
