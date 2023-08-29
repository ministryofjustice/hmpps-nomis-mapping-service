create table non_association_mapping (
    non_association_id  bigint                   not null   primary key ,
    first_offender_no   varchar(10)              not null,
    second_offender_no  varchar(10)              not null,
    nomis_type_sequence int                      not null,
    label               varchar(20),
    mapping_type        varchar(30)              not null,
    when_created        timestamp with time zone not null   default now(),
    constraint non_association_mapping_nomis_id_unique unique (first_offender_no, second_offender_no, nomis_type_sequence)
);

create index non_association_mapping_label_index on non_association_mapping (label);
create index non_association_mapping_when_created_index on non_association_mapping (when_created);
