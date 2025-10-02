CREATE DATABASE auth;
CREATE DATABASE orders;
CREATE DATABASE payments;
CREATE DATABASE catalog;
CREATE DATABASE cart;
CREATE DATABASE restaurant_ops;
CREATE DATABASE delivery;
CREATE DATABASE users_profile;
CREATE DATABASE notification;

-- Connect to auth database and seed roles
\c auth;

-- Create tables if they don't exist
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS users(
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS user_roles
(
    user_id UUID REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (name) VALUES ('CUSTOMER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('RESTAURANT_OWNER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('COURIER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- Create admin user (password: admin123)
INSERT INTO users (id, username, email, password_hash, enabled)
VALUES (
           '550e8400-e29b-41d4-a716-446655440000',
           'admin',
           'admin@fooddelivery.com',
           '$2a$12$tmCACDfa2RevN0GDmNUlsu.51fCAf/MqV41KQob0vZANGMDmprLEK',
           true
       ) ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT '550e8400-e29b-41d4-a716-446655440000', id
FROM roles WHERE name = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Connect to catalog database and seed sample data
\c catalog;

-- Create tables if they don't exist
CREATE TABLE IF NOT EXISTS restaurants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    open BOOLEAN DEFAULT true,
    owner_user_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    section_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price INTEGER NOT NULL,
    available BOOLEAN DEFAULT true,
    version INTEGER DEFAULT 0
);

-- Sample restaurants
INSERT INTO restaurants (id, name, address, open, owner_user_id)
VALUES (
           '550e8400-e29b-41d4-a716-446655440001',
           'Pizza Palace',
           '123 Main St, Food City',
           true,
           '550e8400-e29b-41d4-a716-446655440010'
       ) ON CONFLICT (id) DO NOTHING;

INSERT INTO restaurants (id, name, address, open, owner_user_id)
VALUES (
           '550e8400-e29b-41d4-a716-446655440002',
           'Burger Barn',
           '456 Oak Ave, Food City',
           true,
           '550e8400-e29b-41d4-a716-446655440011'
       ) ON CONFLICT (id) DO NOTHING;

-- Sample menu items for Pizza Palace
INSERT INTO menu_items (id, restaurant_id, section_id, name, description, price, available, version)
VALUES (
           '550e8400-e29b-41d4-a716-446655440101',
           '550e8400-e29b-41d4-a716-446655440001',
            NULL,
           'Margherita Pizza',
           'Fresh mozzarella, tomato sauce, basil',
           1299,
           true,
           0
       ) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu_items (id, restaurant_id, section_id, name, description, price, available, version)
VALUES (
           '550e8400-e29b-41d4-a716-446655440102',
           '550e8400-e29b-41d4-a716-446655440001',
            NULL,
           'Pepperoni Pizza',
           'Pepperoni, mozzarella, tomato sauce',
           1499,
           true,
            0
       ) ON CONFLICT (id) DO NOTHING;

-- Sample menu items for Burger Barn
INSERT INTO menu_items (id, restaurant_id, section_id, name, description, price, available, version)
VALUES (
           '550e8400-e29b-41d4-a716-446655440201',
           '550e8400-e29b-41d4-a716-446655440002',
            NULL,
           'Classic Burger',
           'Beef patty, lettuce, tomato, onion',
           899,
           true,
            0
       ) ON CONFLICT (id) DO NOTHING;

INSERT INTO menu_items (id, restaurant_id, section_id, name, description, price, available, version)
VALUES (
           '550e8400-e29b-41d4-a716-446655440202',
           '550e8400-e29b-41d4-a716-446655440002',
            NULL,
           'Cheese Fries',
           'Crispy fries with melted cheese',
           599,
           true,
            0
       ) ON CONFLICT (id) DO NOTHING;
