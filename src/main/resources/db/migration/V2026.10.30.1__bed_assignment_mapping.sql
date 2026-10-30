create table bed_assignment_mapping
(
    dps_bed_assignment_id uuid                     not null primary key,
    nomis_booking_id      bigint                   not null,
    nomis_sequence        integer                  not null,
    label                 varchar(20),
    mapping_type          varchar(20)              not null,
    when_created          timestamp with time zone not null default now(),
    constraint bed_assignment_mapping_nomis_id_unique unique (nomis_booking_id, nomis_sequence)
);

create index bed_assignment_mapping_label_index on bed_assignment_mapping (label);
create index bed_assignment_mapping_when_created_index on bed_assignment_mapping (when_created);
