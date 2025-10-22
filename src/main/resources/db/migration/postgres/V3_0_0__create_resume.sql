-- V3_0_0__create_resume.sql
-- Create resume storage table with JSONB for flexible nested data structure

CREATE TABLE IF NOT EXISTS resume (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    resume_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

-- Create index for efficient user-based queries
CREATE INDEX idx_resume_user_id ON resume(user_id);

-- Create index for active resumes lookup
CREATE INDEX idx_resume_user_active ON resume(user_id, is_active) WHERE is_active = TRUE;

-- Create GIN index for JSONB queries (enables efficient JSON field searches)
CREATE INDEX idx_resume_data_gin ON resume USING GIN (resume_data);

-- Add trigger for automatic updated_at timestamp
CREATE OR REPLACE FUNCTION update_resume_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_resume_updated_at
    BEFORE UPDATE ON resume
    FOR EACH ROW
    EXECUTE FUNCTION update_resume_updated_at();

-- Add comment for documentation
COMMENT ON TABLE resume IS 'Stores user resumes with flexible JSONB structure for complex nested data';
COMMENT ON COLUMN resume.resume_data IS 'Complete resume data in JSON format matching FinalResume TypeScript interface';
COMMENT ON COLUMN resume.is_active IS 'Flag to mark current active resume (supports draft/published versions)';
COMMENT ON COLUMN resume.version IS 'Optimistic locking version counter, auto-incremented on updates';