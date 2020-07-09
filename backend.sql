CREATE DATABASE bank;

\c bank

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE banks (
    sort_code VARCHAR(50) NOT NULL PRIMARY KEY,
    bank_name VARCHAR(50) NOT NULL,
    UNIQUE(bank_name)
);

CREATE TABLE holders (
    holder_uid UUID NOT NULL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    email VARCHAR(50),
    phone VARCHAR(50),
    country_of_birth VARCHAR(50) NOT NULL,
    UNIQUE(email)
);

CREATE TABLE accounts (
    account_number VARCHAR(50) NOT NULL PRIMARY KEY,
    pin VARCHAR(4) NOT NULL,
    sort_code VARCHAR(50) REFERENCES banks (sort_code),
    holder_uid UUID REFERENCES holders (holder_uid), 
    account_type VARCHAR(50) NOT NULL,
    arranged_overdraft NUMERIC(10,2) NOT NULL CHECK (arranged_overdraft <= 1500 AND arranged_overdraft >= 0),
    balance NUMERIC(10,2) NOT NULL CHECK (balance >  (- arranged_overdraft)),
    currency VARCHAR(10) NOT NULL,
    interest_rate NUMERIC(10,2) NOT NULL CHECK (interest_rate >= -1),
    UNIQUE(holder_uid)
);


CREATE TABLE transactions (
    transaction_uid UUID NOT NULL PRIMARY KEY,
    account_number VARCHAR(50) REFERENCES accounts (account_number) NOT NULL,
    sort_code VARCHAR(50) REFERENCES banks (sort_code) NOT NULL,
    transfer_to_sort_code VARCHAR(50),
    transfer_to_account_number VARCHAR(50) CHECK (transfer_to_account_number <> account_number),
    time_stamp TIMESTAMPTZ NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount <> 0),
    memo VARCHAR(50)
);

insert into banks(sort_code, bank_name) VALUES ('608371', 'Starling');
insert into banks(sort_code, bank_name) VALUES ('090128', 'Santander');
insert into banks(sort_code, bank_name) VALUES ('774026', 'Lloyds');
insert into banks(sort_code, bank_name) VALUES ('878811', 'TSB');
insert into banks(sort_code, bank_name) VALUES ('204742', 'Barclays');
insert into banks(sort_code, bank_name) VALUES ('040004', 'Monzo');
insert into banks(sort_code, bank_name) VALUES ('700119', 'Natwest');
insert into banks(sort_code, bank_name) VALUES ('448308', 'HSBC');
insert into banks(sort_code, bank_name) VALUES ('119999', 'Halifax');
insert into banks(sort_code, bank_name) VALUES ('161525', 'Royal Bank of Scotland');
insert into banks(sort_code, bank_name) VALUES ('080518', 'Bank of Radley');


insert into holders(holder_uid, first_name, last_name, gender, email, phone, country_of_birth) VALUES (uuid_generate_v4(), 'Liam', 'Radley', 'Male', 'liam@radley.net', NULL, 'United Kingdom');
insert into holders(holder_uid, first_name, last_name, gender, email, phone, country_of_birth) VALUES (uuid_generate_v4(), 'Joe', 'Bloggs', 'Male', 'joe@bloggs.net', NULL, 'United Kingdom');

insert into accounts(account_number, pin, sort_code, holder_uid, account_type, arranged_overdraft, balance, currency, interest_rate) VALUES (123456789, '1111', '080518', NULL, 'Current', 0, 4000, 'GBP', .05 );
insert into accounts(account_number, pin, sort_code, holder_uid, account_type, arranged_overdraft, balance, currency, interest_rate) VALUES (467992486, '1234', '608371', NULL, 'Current', 0, 3000, 'GBP', .05 );
