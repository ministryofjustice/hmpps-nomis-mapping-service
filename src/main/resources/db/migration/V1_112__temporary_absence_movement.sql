drop table if exists temporary_absence_movement_mapping;

create table temporary_absence_movement_mapping
(
    dps_movement_id             uuid                     not null PRIMARY KEY,
    nomis_booking_id            bigint                   not null,
    nomis_movement_seq          int                      not null,
    offender_no                 varchar(10)              not null,
    when_created                timestamp with time zone not null default now(),
    when_updated                timestamp with time zone,
    label                       varchar(20),
    mapping_type                varchar(20)              not null,
    nomis_address_id            bigint,
    nomis_address_owner_class   varchar(12),
    dps_address_text            varchar(255)             not null,
    constraint temporary_absence_movement_mapping_nomis_id_unique unique (nomis_booking_id, nomis_movement_seq)
);
create index temporary_absence_movement_mapping_when_created_index on temporary_absence_movement_mapping (when_created);
create index temporary_absence_movement_mapping_label_index on temporary_absence_movement_mapping (label);
create index temporary_absence_movement_mapping_offender_no on temporary_absence_movement_mapping (offender_no);
create index temporary_absence_movement_mapping_booking_id on temporary_absence_movement_mapping (nomis_booking_id, nomis_movement_seq);
create index temporary_absence_movement_mapping_dps_movement_id on temporary_absence_movement_mapping (dps_movement_id);;