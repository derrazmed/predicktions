CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id UUID,
    details TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_admin_audit_logs_admin FOREIGN KEY (admin_id) REFERENCES users(id)
);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs (created_at);
CREATE INDEX idx_admin_audit_logs_admin_created ON admin_audit_logs (admin_id, created_at);
CREATE INDEX idx_admin_audit_logs_target_created ON admin_audit_logs (target_type, target_id, created_at);
CREATE INDEX idx_admin_audit_logs_action_created ON admin_audit_logs (action, created_at);
