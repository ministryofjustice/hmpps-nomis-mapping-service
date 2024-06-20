create table case_note_mapping (
    dps_case_note_id    varchar(36)              not null   primary key,
    nomis_case_note_id  bigint                   not null,
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    when_created        timestamp with time zone not null   default now(),
    constraint case_notes_mapping_nomis_id_unique unique (nomis_case_note_id)
);

create index case_notes_mapping_label_index on case_note_mapping (label);
create index case_notes_mapping_when_created_index on case_note_mapping (when_created);
