create table incentive_mapping
(
    nomis_booking_id               bigint       not null,
    nomis_incentive_sequence       bigint       not null,
    incentive_id                   bigint       not null  PRIMARY KEY,
    label                          varchar(20),
    mapping_type                   varchar(20)  not null,
    when_created                   timestamp with time zone not null default now(),
    constraint incentive_mapping_nomis_id_unique  unique(nomis_booking_id, nomis_incentive_sequence)
);

create index incentive_mapping_label_index on incentive_mapping (label);
create index incentive_mapping_when_created_index on incentive_mapping (when_created);