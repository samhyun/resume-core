-- V3_0_1__remove_resume_user_fk.sql
-- Remove foreign key constraint from resume table
-- User management will be handled by Keycloak, not in our database

-- Drop the foreign key constraint
ALTER TABLE resume DROP CONSTRAINT IF EXISTS fk_resume_user;

-- Update comment to reflect the change
COMMENT ON COLUMN resume.user_id IS 'User identifier from Keycloak (no FK constraint as users are managed externally)';