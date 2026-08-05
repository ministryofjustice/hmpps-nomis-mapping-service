drop table if exists transfer_schedule_mapping;

create table transfer_schedule_mapping
(
    dps_transfer_schedule_id   uuid                     not null PRIMARY KEY,
    nomis_event_id             bigint                   not null,
    offender_no                varchar(10)              not null,
    booking_id                 bigint                   not null,
    when_created               timestamp with time zone not null default now(),
    when_updated               timestamp with time zone,
    label                      varchar(20),
    mapping_type               varchar(20)              not null,
    constraint transfer_schedule_mapping_nomis_id_unique unique (nomis_event_id)
);
create index transfer_schedule_mapping_when_created_index on transfer_schedule_mapping (when_created);
create index transfer_schedule_mapping_label_index on transfer_schedule_mapping (label);
create index transfer_schedule_mapping_offender_no on transfer_schedule_mapping (offender_no);
create index transfer_schedule_mapping_booking_id on transfer_schedule_mapping (booking_id);
create index transfer_schedule_mapping_nomis_event_id on transfer_schedule_mapping (nomis_event_id);