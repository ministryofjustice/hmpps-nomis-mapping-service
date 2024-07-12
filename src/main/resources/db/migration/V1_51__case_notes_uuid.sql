alter table case_note_mapping alter column dps_case_note_id type uuid USING dps_case_note_id::uuid;
