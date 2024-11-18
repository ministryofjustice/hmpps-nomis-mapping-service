alter table case_note_mapping drop constraint case_note_mapping_pkey;
alter table case_note_mapping drop constraint case_notes_mapping_nomis_id_unique;

alter table case_note_mapping add primary key (nomis_case_note_id);
create index case_notes_mapping_dps_id on case_note_mapping (dps_case_note_id);
