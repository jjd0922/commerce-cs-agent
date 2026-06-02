create table orders (
    id varchar(255) not null,
    user_id varchar(255),
    status varchar(255),
    payment_status varchar(255),
    shipment_status varchar(255),
    ordered_at datetime(6),
    delivered_at datetime(6),
    primary key (id)
);

create index idx_orders_user_id on orders (user_id);

create table order_items (
    order_id varchar(255) not null,
    product_id varchar(255),
    product_name varchar(255),
    quantity integer not null,
    unit_price_amount numeric(38, 2),
    unit_price_currency varchar(255),
    constraint fk_order_items_order
        foreign key (order_id)
        references orders (id)
);

create index idx_order_items_order_id on order_items (order_id);

create table returns (
    id varchar(255) not null,
    order_id varchar(255),
    user_id varchar(255),
    reason varchar(255),
    detail varchar(1000),
    requested_at datetime(6),
    status varchar(255),
    idempotency_key varchar(255) not null,
    primary key (id)
);

alter table returns
    add constraint uk_returns_idempotency_key unique (idempotency_key);

create index idx_returns_order_id on returns (order_id);
create index idx_returns_user_id on returns (user_id);

create table outbox_messages (
    id varchar(255) not null,
    aggregate_id varchar(255),
    event_type varchar(255),
    payload text not null,
    status varchar(255),
    retry_count integer not null,
    occurred_at datetime(6),
    created_at datetime(6),
    published_at datetime(6),
    primary key (id)
);

create index idx_outbox_messages_status_created_at
    on outbox_messages (status, created_at);
