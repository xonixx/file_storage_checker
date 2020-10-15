#!/usr/bin/env bash

echo "
create database file_storage_checker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
grant all privileges on file_storage_checker.* to 'file_storage_checker'@'%' identified by 'file_storage_checker';
flush privileges;
" | mysql -uroot -p
