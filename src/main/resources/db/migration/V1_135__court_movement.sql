drop table if exists court_movement_mapping;

create table court_movement_mapping
(
    dps_court_movement_id      uuid                     not null PRIMARY KEY,
    nomis_booking_id            bigint                   not null,
    nomis_movement_seq          int                      not null,
    offender_no                varchar(10)              not null,
    when_created               timestamp with time zone not null default now(),
    when_updated               timestamp with time zone,
    label                      varchar(20),
    mapping_type               varchar(20)              not null,
    constraint court_movement_mapping_nomis_id_unique unique (nomis_booking_id, nomis_movement_seq)
);
create index court_movement_mapping_when_created_index on court_movement_mapping (when_created);
create index court_movement_mapping_label_index on court_movement_mapping (label);
create index court_movement_mapping_offender_no on court_movement_mapping (offender_no);
create index court_movement_mapping_nomis_id on court_movement_mapping (nomis_booking_id, nomis_movement_seq);