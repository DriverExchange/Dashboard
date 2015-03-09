-- shift stats

drop table if exists dashboard_shifts;

create table dashboard_shifts as
	(select
		si.id as site_id
		,(select count(sh.id)
			from shifts sh
			join shift_templates st on st.id = sh.shifttemplate_id
			where st.site_id = si.id
			and sh.bookedstarttime is not null
			and now() - sh.bookedstarttime > interval '12 hours' 
			and (sh.actstarttime is null or sh.actstoptime is null)
			and sh.cancellationdate is null) as start_stop
		,(select count(sh.id)
			from timesheets t
			join shift_templates st on st.id = t.shifttemplate_id
			join shifts sh on sh.shifttemplate_id = st.id
			where t.dtype = 'ExchangeTimesheets'
			and sh.approveddate is null
			and t.enddate < now()
			and sh.approveddate is null
			and st.site_id = si.id) as unapproved
		,(select count(sh.id)
			from timesheets t
			join shift_templates st on st.id = t.shifttemplate_id
			join shifts sh on sh.shifttemplate_id = st.id
			where t.dtype = 'ExchangeTimesheets'
			and sh.approveddate is null
			and t.enddate < now()
			and t.purchaseorderref is null
			and st.site_id = si.id) as no_po

	from
		sites si

	where

		si.archived is false
		and not exists (select 1
			from agency_managed_sites ams where ams.site_id = si.id)
		and (exists (select 1
				from shifts sh
				join shift_templates st on st.id = sh.shifttemplate_id
				where st.site_id = si.id
				and sh.bookedstarttime is not null
				and now() - sh.bookedstarttime > interval '12 hours' 
				and (sh.actstarttime is null or sh.actstoptime is null)
				and sh.cancellationdate is null)
			or
			(exists (select 1
				from timesheets t
				join shift_templates st on st.id = t.shifttemplate_id
				where t.dtype = 'ExchangeTimesheets'
				and t.enddate < now()
				and t.purchaseorderref is null
				and st.site_id = si.id
				))
			or
			(exists (select 1
				from timesheets t
				join shift_templates st on st.id = t.shifttemplate_id
				join shifts sh on sh.shifttemplate_id = st.id
				where t.dtype = 'ExchangeTimesheets'
				and t.enddate < now()
				and sh.approveddate is null
				and st.site_id = si.id
				))
			)
	);