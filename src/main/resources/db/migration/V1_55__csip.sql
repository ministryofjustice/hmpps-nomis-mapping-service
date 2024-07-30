alter table csip_mapping add column
    offender_no      varchar(10) not null;

create index csip_mapping_offender_no_index ON csip_mapping (offender_no);

create table csip_prisoner_mapping
(
    offender_no  varchar(10)              not null PRIMARY KEY,
    count        bigint                   not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null
);
create index csip_prisoner_mapping_label_index on csip_prisoner_mapping (label);
