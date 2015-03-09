-- notification stats grouped by site 

drop table if exists dashboard_notifications;

create table dashboard_notifications as
	(select
		n.tosite_id
		,count(n.id) as notifications
		,(select count(sh.id)
			from shifts sh
			join shift_templates st on st.id = sh.shifttemplate_id
			where sh.bookedstarttime > now() - interval '1 month'
			and n.tosite_id = st.site_id) as posted_shifts
		,(select count(sh.id)
			from shifts sh
			join shift_templates st on st.id = sh.shifttemplate_id
			where sh.acceptedbid_id is not null
			and sh.bookedstarttime > now() - interval '1 month'
			and n.tosite_id = st.site_id) as filled_shifts
	from
		notifications n

	where
		n.type in ('DRIVER_QUERIES_SHIFT',
		'USER_CONTACTS_SITE',
		'DRIVER_CONTACTS_BUYER_RE_OPEN_SHIFT',
		'DRIVER_CONTACTS_BUYER_RE_AWARDED_SHIFT',
		'DRIVER_REPLIES_TO_NOTIFICATION',
		'DRIVER_CONTACTS_SITE',
		'DRIVER_REQUESTS_ASSESSMENT',
		'DRIVER_CONTACTS_BUYER_RE_ASSESSMENT')
		and n.createddate > now() - interval '4 weeks'
		and n.createddate = n.modifieddate
		and not exists (select 1 from agency_managed_sites ams where ams.site_id = n.tosite_id)
	group by
		n.tosite_id
	order by notifications desc);