-- common dashboard items

drop table if exists dashboard_common;
create table dashboard_common as (select
	o.id as org_id
	,o.dtype as org_dtype
	,o.name as org_name
	,o.createddate as org_createddate
	,o.archived as org_archived
	,o.isdirect as org_isdirect
	,coalesce(acc.firstname || ' ' || acc.surname, 'None assigned') as account_manager
	,si.id as site_id
	,si.name as site_name
	,si.slug site_slug
	,si.createddate as site_createddate
	,si.archived as site_archived
	,u.firstname || ' ' || u.surname as created_by
	,u.email
	,u.otherphone as phone_number
	,sa.postcode
	,sa.county
	,r.name as region_name
	,oa.postcode as org_postcode
	,oa.county as org_county
	,orgr.name as org_region_name
	,(select sum(case
		when ST_DWithin(sa.geog, wa.geog, 25 * 1609.344)
		then 1
		else 0
		end)
		from driver_resumes dr
		join addresses wa on wa.id = dr.address_id
		join users u on u.driverresume_id = dr.id
		,sites si2
		join addresses sa on si2.address_id = sa.id
		where si.id = si2.id
		and u.archived is false
		and exists (select 1 from user_roles ur where ur.role = 'DIRECT' and ur.user_id = u.id)
		and (wa.longitude is not null and wa.latitude is not null)) as drivers_in_25_miles
	,(select sum(case
		when ST_DWithin(oa.geog, wa.geog, 25 * 1609.344)
		then 1
		else 0
		end)
		from driver_resumes dr
		join addresses wa on wa.id = dr.address_id
		join users u on u.driverresume_id = dr.id
		,organisations o2
		join addresses oa on o.headofficeaddress_id = oa.id
		where o.id = o2.id
		and u.archived is false
		and exists (select 1 from user_roles ur where ur.role = 'DIRECT' and ur.user_id = u.id)
		and (wa.longitude is not null and wa.latitude is not null)) as org_drivers_in_25_miles
	from organisations o
		left join sites si on si.organisation_id = o.id
		left join addresses sa on sa.id = si.address_id
		left join regions r on r.id = sa.region_id
		left join users u on u.organisation_id = o.id
		left join users acc on o.accountmanager_id = acc.id
		left join addresses oa on o.headofficeaddress_id = oa.id
		left join regions orgr on orgr.id = oa.region_id
	where
		u.id = (select min(u1.id)
			from users u1
			where u1.organisation_id = o.id)
	);