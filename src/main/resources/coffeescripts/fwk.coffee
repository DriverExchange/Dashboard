
window.fwk = window.fwk || {}

_.extend window.fwk, do ->

	data: {}

	views: do ->
		templates = {}
		templatePaths = []

		add: (templatesToAdd) ->
			templates = _.extend(templates, templatesToAdd)
			for key, value of templatesToAdd
				templatePaths.push(value)

		addInline: (templatesToAdd) ->
			for key, value of templatesToAdd
				fwk.views[key] = ejs.compile(value, filename: key + ".ejs")

	domEvents: do ->
		add: (options) ->
			for eventSelector, events of options
				for eventType, callback of events
					$(document).on(eventType, eventSelector, callback)
