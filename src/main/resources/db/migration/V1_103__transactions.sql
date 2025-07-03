create table transaction_mapping
(
    dps_transaction_id   uuid                     not null primary key,
    nomis_transaction_id bigint                   not null,
    offender_no          varchar(10)              not null,
    nomis_booking_id     bigint,
    label                varchar(20),
    mapping_type         varchar(20)              not null,
    when_created         timestamp with time zone not null default now(),
    constraint transactions_mapping_nomis_id_unique unique (nomis_transaction_id)
);

create index transaction_mapping_label_index on transaction_mapping (label);
create index transaction_mapping_when_created_index on transaction_mapping (when_created);
create index transaction_mapping_offender_no_index ON transaction_mapping (offender_no);
create index transaction_mapping_nomis_booking_id_index ON transaction_mapping (nomis_booking_id);
