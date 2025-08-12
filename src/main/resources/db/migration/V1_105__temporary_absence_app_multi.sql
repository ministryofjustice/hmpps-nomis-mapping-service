create table temporary_absence_app_multi_mapping
(
    dps_app_multi_id        uuid                     not null PRIMARY KEY,
    nomis_app_multi_id      bigint                   not null,
    offender_no             varchar(10)              not null,
    booking_id              bigint                   not null,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null,
    constraint temporary_absence_app_multi_mapping_nomis_id_unique unique (nomis_app_multi_id)
);
create index temporary_absence_app_multi_mapping_when_created_index on temporary_absence_app_multi_mapping (when_created);
create index temporary_absence_app_multi_mapping_label_index on temporary_absence_app_multi_mapping (label);
create index temporary_absence_app_multi_mapping_offender_no on temporary_absence_app_multi_mapping (offender_no);
create index temporary_absence_app_multi_mapping_booking_id on temporary_absence_app_multi_mapping (booking_id);
