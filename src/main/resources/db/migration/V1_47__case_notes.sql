alter table case_note_mapping add column  offender_no      varchar(10) not null;
alter table case_note_mapping add column  nomis_booking_id bigint      not null;

create index case_note_mapping_offender_no_index ON case_note_mapping (offender_no);
create index case_note_mapping_nomis_booking_id_index ON case_note_mapping (nomis_booking_id);
