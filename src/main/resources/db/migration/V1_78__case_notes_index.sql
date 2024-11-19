create index if not exists case_notes_mapping_dps_id on case_note_mapping (dps_case_note_id);
alter table case_note_mapping drop constraint case_note_mapping_pkey;
