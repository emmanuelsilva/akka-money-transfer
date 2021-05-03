CREATE TABLE IF NOT EXISTS checking_accounts(
    id INT AUTO_INCREMENT PRIMARY KEY,
    version INT NOT NULL,
    iban VARCHAR(34) NOT NULL,
    currency CHAR(3) NOT NULL,
    customer_id INT NOT NULL,
    customer_name VARCHAR(255) NOT NULL
);