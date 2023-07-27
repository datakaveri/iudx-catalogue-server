CREATE TABLE IF NOT EXISTS auditing_rs_withtype_temp
(
   id varchar NOT NULL,
   api varchar NOT NULL,
   userid varchar NOT NULL,
   epochtime numeric NOT NULL,
   resourceid varchar NOT NULL,
   isotime varchar NOT NULL,
   time timestamp without time zone,
   providerid varchar NOT NULL,
   resourcegroup varchar,
   size numeric NOT NULL
);

INSERT INTO auditing_rs (id, api, userid, epochtime, resourceid, isotime, providerid, resourcegroup, size, time) VALUES ('a462ae274cc242688486adfdb2c853cf', '/ngsi-ld/v1/entities', '15c7506f-c800-48d6-adeb-0542b03947c6', 1671443074549, 'iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta', '2022-12-19T15:14:34+05:30[Asia/Kolkata]', 'iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86', 'dummy-rg-id', 79, '2022-12-19 09:44:34');