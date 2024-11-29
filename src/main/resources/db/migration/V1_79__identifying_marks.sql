-- Maps OFFENDER_IDENTIFYING_MARK unique key (nomis_booking_id, nomis_marks_sequence) to the corresponding DPS id.
-- Note that many NOMIS marks may point to a single DPS mark as this is how they are handling marks copied to new bookings.
create table identifying_mark_mapping
(
    nomis_booking_id     bigint,
    nomis_marks_sequence bigint,
    dps_id               uuid                     not null,
    offender_no          varchar(10)              not null,
    label                varchar(20),
    when_created         timestamp with time zone not null default now(),
    mapping_type         varchar(20)              not null,
    primary key (nomis_booking_id, nomis_marks_sequence)
);
create index identifying_mark_mapping_when_created_index on identifying_mark_mapping (when_created);
create index identifying_mark_mapping_label_index on identifying_mark_mapping (label);
create index identifying_mark_mapping_dps_id_index on identifying_mark_mapping (dps_id);
create index identifying_mark_mapping_offender_no_index on identifying_mark_mapping (offender_no);
