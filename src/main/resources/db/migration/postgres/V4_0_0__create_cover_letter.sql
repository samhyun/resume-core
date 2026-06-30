-- V4_0_0__create_cover_letter.sql
-- Create cover_letter storage table. Plain columns (content is free text, not JSONB).
-- No is_active concept: a user keeps a collection of cover letters (unlike resume's single-active model).

CREATE TABLE IF NOT EXISTS cover_letter (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    resume_id UUID,
    company_name VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    job_description TEXT,
    content TEXT NOT NULL,
    validation_score INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

-- Index for efficient user-based list queries
CREATE INDEX idx_cover_letter_user_id ON cover_letter(user_id);

-- Index for filtering cover letters by the resume they were generated from
CREATE INDEX idx_cover_letter_resume_id ON cover_letter(resume_id);

-- Auto-maintain updated_at + version on UPDATE (mirrors resume table behaviour)
CREATE OR REPLACE FUNCTION update_cover_letter_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_cover_letter_updated_at
    BEFORE UPDATE ON cover_letter
    FOR EACH ROW
    EXECUTE FUNCTION update_cover_letter_updated_at();

COMMENT ON TABLE cover_letter IS 'Stores generated/edited cover letters for users';
COMMENT ON COLUMN cover_letter.resume_id IS 'Resume this cover letter was generated from (nullable, no FK as resumes may be deleted)';
COMMENT ON COLUMN cover_letter.content IS 'Cover letter body text (agent draft_cover_letter.full_text)';
COMMENT ON COLUMN cover_letter.validation_score IS 'Agent validation score (validation_result.total_score), nullable';
COMMENT ON COLUMN cover_letter.user_id IS 'User identifier from Keycloak (no FK constraint as users are managed externally)';
