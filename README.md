#  Tiny Store Manager

**Work in progress**

* Converting a offer file from Bol.com shop-in-shop to an csv file suitable for importing into Woocommerce.
* Enrich list of isbn's with metadata using LibraryThing

# Setting up

Create database tiny_store_manager, using the code like below:
```sql
create database tiny_store_manager;
CREATE USER tiny_store_manager  WITH PASSWORD 'pass123';
GRANT ALL PRIVILEGES ON DATABASE tiny_store_manager to tiny_store_manager;
```
