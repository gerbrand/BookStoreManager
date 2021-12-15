create table product_entry
(
    reference text not null,
    ean text not null
        constraint product_entry_pk primary key,
    condition text not null,
    stock int8 not null,
    price numeric not null,
    delivery_code text not null,
    description text,
    long_description text,
    for_sale Boolean not null default false,
    title text,
    images text[]
);
