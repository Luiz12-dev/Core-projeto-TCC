-- V5: Create blocked_periods table
CREATE TABLE IF NOT EXISTS blocked_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blocked_periods_range ON blocked_periods(start_date_time, end_date_time);
