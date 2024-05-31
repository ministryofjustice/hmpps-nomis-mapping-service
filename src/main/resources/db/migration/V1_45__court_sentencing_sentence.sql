create table sentence_mapping
(
    nomis_booking_id        bigint                   not null,
    nomis_sentence_sequence int                      not null,
    dps_sentence_id     varchar                  not null primary key,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint sentence_mapping_nomis_id_unique unique (nomis_booking_id, nomis_sentence_sequence)
);
create index sentence_mapping_when_created_index on sentence_mapping (when_created);
create index sentence_mapping_label_index on sentence_mapping (label);
