CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_auth_sessions_expiry ON auth_sessions(expires_at);
CREATE INDEX idx_auth_sessions_user ON auth_sessions(user_id);

-- Somente o SHA-256 de segredos aleatórios de 256 bits; nunca o refresh em claro.
-- Tokens consumidos permanecem até a expiração da sessão para detectar reutilização.
CREATE TABLE auth_refresh_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES auth_sessions(id) ON DELETE CASCADE,
    consumed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_auth_refresh_session ON auth_refresh_tokens(session_id);
