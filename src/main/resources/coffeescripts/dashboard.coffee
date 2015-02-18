
getCurrentWidgetHeight = ->
	if fwk.data.dashboardConf.gridSize
		Math.floor(($(window).height() - 70) / fwk.data.dashboardConf.gridSize.y)
	else
		null

windowResize = ->
	fwk.data.currentWidgetHeight = getCurrentWidgetHeight()
	updateWidgetWidgetHeight()

updateWidgetWidgetHeight = (widgetName) ->
	if fwk.data.currentWidgetHeight
		widgetSelector = if widgetName then "[data-name=#{widgetName}]" else ""
		$(".widget" + widgetSelector)
			.css("height", fwk.data.currentWidgetHeight)
			.find(".box")
			.css("height", fwk.data.currentWidgetHeight - 20)
			.find(".boxBody")
			.css("height", fwk.data.currentWidgetHeight - 120)

fwk.data.currentWidgetHeight = getCurrentWidgetHeight()

updateWidgetData = (widgetName) ->
	$.ajax
		url: "/widgets/#{widgetName}"
		success: (widget) ->
			if widget.css
				$("head").append """<style type="text/css">#{widget.css}</style>"""
			if widget.template
				template = ejs.compile(widget.template, filename: widget.name + "_widget.ejs")
				html = template(widget: widget)
			else
				html = fwk.views.widgetTable(widget: widget)
			$(".widget[data-name=#{widget.name}]").replaceWith(html)
			updateWidgetWidgetHeight(widgetName)
			$(".grid .widget[data-name=#{widget.name}] .boxBody").mCustomScrollbar(theme: "minimal-dark")

		error: (xhr) ->
			if xhr.responseText[0] == "{" || xhr.responseText[0] == "["
				fwk.views.widget(widget: {name: widgetName}, errors: $.parseJSON(xhr.responseText))

$ ->

	$("body").addClass(fwk.data.dashboardConf.type)

	$("#topBar").replaceWith fwk.views.topBar({})

	$widgets = $("#widgets")
	nbCols = fwk.data.dashboardConf.widgets.length
	for cols, idx in fwk.data.dashboardConf.widgets
		$widgets.append("""<div class="col col#{idx} nbCols#{nbCols}"></div>""")
		$col = $widgets.find(".col.col#{idx}")
		for widgetName in cols
			widget =
				name: widgetName
				configuration:
					title: fwk.data.widgetTitles[widgetName]
			$col.append(fwk.views.widget(widget: widget))
			$(".widget[updateWidgetData-name=#{widgetName}]").spinStart(largeSpinnerOptions)
			updateWidgetWidgetHeight(widgetName)
			updateWidgetData(widgetName)

	if fwk.data.dashboardConf.type == "grid"
		$(window).resize(windowResize)

