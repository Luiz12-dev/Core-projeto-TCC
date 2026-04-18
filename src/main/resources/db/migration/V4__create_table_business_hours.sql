-- V4: Create business_hours table
CREATE TABLE IF NOT EXISTS business_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_of_week VARCHAR(10) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_business_hours_day_open UNIQUE (day_of_week, open_time)
);
