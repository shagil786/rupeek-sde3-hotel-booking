create table owners (
    id varchar(36) primary key,
    username varchar(120) not null unique,
    created_at timestamp not null
);

create table properties (
    id varchar(36) primary key,
    owner_id varchar(36) not null,
    name varchar(200) not null,
    city varchar(100) not null,
    locality varchar(150) not null,
    star_rating integer not null,
    amenities varchar(1000) not null,
    constraint fk_property_owner foreign key (owner_id) references owners(id)
);

create table room_types (
    id varchar(36) primary key,
    property_id varchar(36) not null,
    name varchar(120) not null,
    capacity integer not null,
    price_per_night decimal(12,2) not null,
    inventory_count integer not null,
    version bigint not null,
    constraint fk_room_property foreign key (property_id) references properties(id)
);

create table bookings (
    id varchar(36) primary key,
    room_type_id varchar(36) not null,
    customer_username varchar(120) not null,
    check_in date not null,
    check_out date not null,
    guests integer not null,
    amount decimal(12,2) not null,
    status varchar(32) not null,
    idempotency_key varchar(200),
    version bigint not null,
    created_at timestamp not null,
    constraint fk_booking_room foreign key (room_type_id) references room_types(id),
    constraint uq_booking_idempotency unique (customer_username, idempotency_key)
);

create table payments (
    id varchar(36) primary key,
    booking_id varchar(36) not null unique,
    method varchar(32) not null,
    amount decimal(12,2) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    constraint fk_payment_booking foreign key (booking_id) references bookings(id)
);
