alter table manifest_item  modify modified datetime(3) not null;
alter table audit_log_item modify modified datetime(3) not null;
alter table bit_report modify modified datetime(3) not null;
alter table bit_log_item  modify modified datetime(3) not null;

