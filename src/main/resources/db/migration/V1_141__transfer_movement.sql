drop table if exists transfer_movement_mapping;

create table transfer_movement_mapping
(
    dps_transfer_movement_id   uuid                     not null PRIMARY KEY,
    nomis_booking_id            bigint                   not null,
    nomis_movement_seq          int                      not null,
    offender_no                varchar(10)              not null,
    when_created               timestamp with time zone not null default now(),
    when_updated               timestamp with time zone,
    label                      varchar(20),
    mapping_type               varchar(20)              not null,
    constraint transfer_movement_mapping_nomis_id_unique unique (nomis_booking_id, nomis_movement_seq)
);
create index transfer_movement_mapping_when_created_index on transfer_movement_mapping (when_created);
create index transfer_movement_mapping_label_index on transfer_movement_mapping (label);
create index transfer_movement_mapping_offender_no on transfer_movement_mapping (offender_no);
create index transfer_movement_mapping_nomis_id on transfer_movement_mapping (nomis_booking_id, nomis_movement_seq);
