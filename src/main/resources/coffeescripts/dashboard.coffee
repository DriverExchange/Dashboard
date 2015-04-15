window.g = window.g || {}
g.charts = []

getCurrentWidgetHeight = ->
	if fwk.data.dashboardConf.gridSize
		Math.floor(($(window).height() - 70) / fwk.data.dashboardConf.gridSize.y)
	else
		null

windowResize = ->
	fwk.data.currentWidgetHeight = getCurrentWidgetHeight()
	updateWidgetHeight()

updateWidgetHeight = (widgetName) ->
	if fwk.data.currentWidgetHeight
		widgetSelector = if widgetName then "[data-name=#{widgetName}]" else ""
		$widget = $(".widget" + widgetSelector)
		$widget
			.css("height", fwk.data.currentWidgetHeight)
			.find(".box")
			.css("height", fwk.data.currentWidgetHeight - 20)
			.find(".boxBody")
		if $(".keys").length > 0
			$widget.find(".boxBody").css("height", fwk.data.currentWidgetHeight - 150)
		else
			$widget.find(".boxBody").css("height", fwk.data.currentWidgetHeight - 120)
		$(".modal .body")
			.css("max-height", $(window).height() - 170)
			.mCustomScrollbar("update")
		$(".modal").css(top: Math.floor(($(window).height() - $(".modal").outerHeight()) / 2))

fixKeysWidth = (widgetName) ->
	if fwk.data.currentWidgetHeight
		widgetSelector = if widgetName then "[data-name=#{widgetName}]" else ""
		$keysHolder = $(".widget" + widgetSelector + " .keys")
		$keys = $keysHolder.find(".key")
		totalWidth = 40
		$keys.each(-> totalWidth += $(this).width() + 12)
		$keysHolder.width(totalWidth)

fwk.data.currentWidgetHeight = getCurrentWidgetHeight()

updateWidgetData = (widgetName, global) ->
	$.ajax
		url: "/widgets/#{widgetName}/global/#{global}"
		success: (widget) ->
			if widget.css
				$("head").append """<style type="text/css">#{widget.css}</style>"""
			if widget.template
				template = ejs.compile(widget.template, filename: widget.name + "_widget.ejs")
				html = template(widget: widget)
			else
				html = fwk.views.widgetTable(widget: widget)
			$(".widget[data-name=#{widget.name}]").replaceWith(html)
			updateWidgetHeight(widgetName)
			fixKeysWidth(widgetName)
			$(".grid .widget[data-name=#{widget.name}] .boxBody").mCustomScrollbar(theme: "minimal-dark")

		error: (xhr) ->
			if xhr.responseText[0] == "{" || xhr.responseText[0] == "["
				fwk.views.widget(widget: {name: widgetName}, errors: $.parseJSON(xhr.responseText))

fwk.domEvents.add
	"[data-modal-data]": click: ->
		html = fwk.views.genericDataModal
			title: $(this).data("modal-title")
			tableConfiguration: $(this).data("data-modal-table-configuration") || $(this).closest("[data-modal-table-configuration]").data("modal-table-configuration")
			data: $(this).data("modal-data")
		$("#modalHolder").html(html)
		updateWidgetHeight()
		$(".modal .body").mCustomScrollbar(theme: "minimal-dark")

	".modalBackground, .modal .close": click: ->
		$("#modalHolder").empty()

	"[data-modal-ajax] .tag, [data-modal-ajax].tag": click: ->
		$that = $(this)
		$(".topBarSpinnerHolder").spinStart(topBarSpinnerOptions)
		$.ajax
			url: $that.data("modal-ajax-url")
			method: "GET"
			dataType: "JSON"
			success: (modalData) ->
				mergedWidget = {}
				_.each(modalData, (data) -> _.extend(mergedWidget, data))
				html = fwk.views.genericDataModal
					title: $that.data("modal-title")
					tableConfiguration: $that.data("data-modal-table-configuration") || $that.closest("[data-modal-table-configuration]").data("modal-table-configuration")
					data: mergedWidget
				$("#modalHolder").html(html)
				updateWidgetHeight()
				$(".modal .body").mCustomScrollbar(theme: "minimal-dark")
			error: (xhr) ->
				if xhr.responseText[0] == "{" || xhr.responseText[0] == "["
					fwk.views.widget(widget: {name: widgetName}, errors: $.parseJSON(xhr.responseText))
			complete: ->
				$(".topBarSpinnerHolder").spinStop()

$(document).keyup (e) ->
	if e.keyCode == 27
		$("#modalHolder").empty()

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
			$col.append(fwk.views.widget(widget: widget, loading: true))
			$(".widget[data-name=#{widgetName}] .box").spinStart(largeSpinnerOptions)
			updateWidgetHeight(widgetName)
			updateWidgetData(widgetName, fwk.data.dashboardConf.global.toLowerCase() == "true")

	if fwk.data.dashboardConf.type == "grid"
		$(window).resize(windowResize)
