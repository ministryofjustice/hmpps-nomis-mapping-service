-- GL_TRANSACTIONS can exist without an offender
alter table transaction_mapping alter offender_no DROP NOT NULL;
