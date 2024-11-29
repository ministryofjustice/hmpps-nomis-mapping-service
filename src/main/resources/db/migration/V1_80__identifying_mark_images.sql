-- Maps OFFENDER_IMAGES for identifying marks to the corresponding DPS id.
-- Note that there will always be a 1:1 mapping between NOMIS and DPS images (images aren't copied to new bookings in NOMIS).
create table identifying_mark_image_mapping
(
    nomis_offender_image_id bigint                   primary key,
    dps_id                  uuid                     unique,
    nomis_booking_id        bigint                   not null,
    nomis_marks_sequence    bigint                   not null,
    offender_no             varchar(10)              not null,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null
);
create index identifying_mark_image_mapping_dps_id_index on identifying_mark_image_mapping (dps_id);
create index identifying_mark_image_mapping_when_created_index on identifying_mark_image_mapping (when_created);
create index identifying_mark_image_mapping_label_index on identifying_mark_image_mapping (label);
create index identifying_mark_image_mapping_offender_no_index on identifying_mark_image_mapping (offender_no);
