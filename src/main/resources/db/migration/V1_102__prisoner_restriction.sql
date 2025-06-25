create table prisoner_restriction_mapping
(
    dps_id       varchar(36)              not null PRIMARY KEY,
    nomis_id     bigint                   not null,
    offender_no  varchar(10)              not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null,
    constraint prisoner_restriction_mapping_nomis_id_unique unique (nomis_id)
);
create index prisoner_restriction_mapping_when_created_index on prisoner_restriction_mapping (when_created);
create index prisoner_restriction_mapping_label_index on prisoner_restriction_mapping (label);
create index prisoner_restriction_mapping_offender_no on prisoner_restriction_mapping (offender_no);
