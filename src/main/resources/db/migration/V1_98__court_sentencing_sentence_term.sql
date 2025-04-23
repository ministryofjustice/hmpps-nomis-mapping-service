create table sentence_term_mapping
(
    nomis_booking_id        bigint                   not null,
    nomis_sentence_sequence int                      not null,
    nomis_term_sequence int                      not null,
    dps_term_id         varchar                  not null primary key,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint sentence_term_mapping_nomis_id_unique unique (nomis_booking_id, nomis_sentence_sequence, nomis_term_sequence)
);
create index sentence_term_mapping_when_created_index on sentence_term_mapping (when_created);
create index sentence_term_mapping_label_index on sentence_term_mapping (label);
