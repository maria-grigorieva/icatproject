-- defferable constraint allows facility_useR_id to be updated
PROMPT deferrable constraint on investigator table

ALTER TABLE investigator DROP CONSTRAINT INVESTIGATOR_FACILITY_USER_FK1;

ALTER TABLE INVESTIGATOR
ADD CONSTRAINT INVESTIGATOR_FACILITY_USER_FK1 FOREIGN KEY
(
FACILITY_USER_ID
)
REFERENCES FACILITY_USER
(
FACILITY_USER_ID
) DEFERRABLE INITIALLY IMMEDIATE ENABLE
;

