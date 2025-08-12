create table temporary_absence_schedule_mapping
(
    dps_schedule_id         uuid                     not null PRIMARY KEY,
    nomis_schedule_id       bigint                   not null,
    offender_no             varchar(10)              not null,
    booking_id              bigint                   not null,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null,
    constraint temporary_absence_schedule_mapping_nomis_id_unique unique (nomis_schedule_id)
);
create index temporary_absence_schedule_mapping_when_created_index on temporary_absence_schedule_mapping (when_created);
create index temporary_absence_schedule_mapping_label_index on temporary_absence_schedule_mapping (label);
create index temporary_absence_schedule_mapping_offender_no on temporary_absence_schedule_mapping (offender_no);
create index temporary_absence_schedule_mapping_booking_id on temporary_absence_schedule_mapping (booking_id);
