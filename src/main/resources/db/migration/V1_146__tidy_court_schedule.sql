delete from court_schedule_mapping csm
  where exists (select 1 from court_appearance_mapping cam where csm.nomis_event_id = cam.nomis_court_appearance_id)