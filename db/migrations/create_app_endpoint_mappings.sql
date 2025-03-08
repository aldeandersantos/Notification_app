CREATE TABLE app_endpoint_mappings (
    id SERIAL PRIMARY KEY,
    app_name VARCHAR(255) NOT NULL,
    endpoint_id INTEGER REFERENCES webhook_endpoints(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(app_name, endpoint_id)
); 