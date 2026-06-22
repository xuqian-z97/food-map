create sequence if not exists user_profile_id_seq start with 300001 increment by 1;
comment on sequence user_profile_id_seq is '用户资料业务主键序列，用于 user_profiles.profile_id';

create sequence if not exists user_setting_id_seq start with 400001 increment by 1;
comment on sequence user_setting_id_seq is '用户设置业务主键序列，用于 user_settings.setting_id';
