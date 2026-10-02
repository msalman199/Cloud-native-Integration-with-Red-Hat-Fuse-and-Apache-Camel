CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE integration_logs (
    id SERIAL PRIMARY KEY,
    route_id VARCHAR(100),
    message_id VARCHAR(100),
    status VARCHAR(20),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO customers (name, email, phone) VALUES
('John Doe', 'john.doe@email.com', '+1-555-0101'),
('Jane Smith', 'jane.smith@email.com', '+1-555-0102'),
('Bob Johnson', 'bob.johnson@email.com', '+1-555-0103');

INSERT INTO orders (customer_id, order_number, total_amount, status) VALUES
(1, 'ORD-001', 299.99, 'PENDING'),
(2, 'ORD-002', 149.50, 'PROCESSING'),
(3, 'ORD-003', 89.99, 'COMPLETED');
