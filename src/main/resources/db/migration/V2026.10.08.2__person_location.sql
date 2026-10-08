drop index prisoner_location_booking_mapping_when_created_index;
drop index prisoner_location_booking_mapping_label_index;
drop table prisoner_location_booking_mapping;
drop index prisoner_location_movement_mapping_when_created_index;
drop index prisoner_location_movement_mapping_label_index;
drop table prisoner_location_movement_mapping;
drop table prisoner_location_migration;

-- Recreate everything as person instead of prisoner to match DPS naming
create table person_location_booking_mapping
(
    dps_custodial_series_id uuid                     not null PRIMARY KEY,
    nomis_booking_id        bigint                   not null,
    when_created            timestamp with time zone not null default now(),
    when_updated            timestamp with time zone,
    label                   varchar(20),
    mapping_type            varchar(20)              not null,
    constraint person_location_booking_mapping_nomis_id_unique unique (nomis_booking_id)
);
create index person_location_booking_mapping_when_created_index on person_location_booking_mapping (when_created);
create index person_location_booking_mapping_label_index on person_location_booking_mapping (label);

create table person_location_movement_mapping
(
    dps_external_movement_id uuid                     not null PRIMARY KEY,
    nomis_booking_id         bigint                   not null,
    nomis_movement_seq       int                      not null,
    when_created             timestamp with time zone not null default now(),
    when_updated             timestamp with time zone,
    label                    varchar(20),
    mapping_type             varchar(20)              not null,
    constraint person_location_movement_mapping_nomis_id_unique unique (nomis_booking_id, nomis_movement_seq)
);
create index person_location_movement_mapping_when_created_index on person_location_movement_mapping (when_created);
create index person_location_movement_mapping_label_index on person_location_movement_mapping (label);

create table person_location_migration
(
    offender_no  varchar(10)              not null PRIMARY KEY,
    when_created timestamp with time zone not null default now(),
    label        varchar(20)
);
