drop table if exists temporary_absence_schedule_mapping;

create table temporary_absence_schedule_mapping
(
    dps_occurrence_id          uuid                     not null PRIMARY KEY,
    nomis_event_id             bigint                   not null,
    offender_no                varchar(10)              not null,
    booking_id                 bigint                   not null,
    when_created               timestamp with time zone not null default now(),
    when_updated               timestamp with time zone,
    label                      varchar(20),
    mapping_type               varchar(20)              not null,
    nomis_address_id           bigint                   not null,
    nomis_address_owner_class  varchar(12)              not null,
    dps_address_text           varchar(255)             not null,
    event_time                 timestamp with time zone not null,
    constraint temporary_absence_schedule_mapping_nomis_id_unique unique (nomis_event_id)
);
create index temporary_absence_schedule_mapping_when_created_index on temporary_absence_schedule_mapping (when_created);
create index temporary_absence_schedule_mapping_label_index on temporary_absence_schedule_mapping (label);
create index temporary_absence_schedule_mapping_offender_no on temporary_absence_schedule_mapping (offender_no);
create index temporary_absence_schedule_mapping_booking_id on temporary_absence_schedule_mapping (booking_id);
create index temporary_absence_schedule_mapping_dps_schedule_id on temporary_absence_schedule_mapping (dps_occurrence_id);
create index temporary_absence_schedule_mapping_nomis_schedule_id on temporary_absence_schedule_mapping (nomis_event_id);
create index temporary_absence_schedule_mapping_address_event on temporary_absence_schedule_mapping (nomis_address_id, event_time);