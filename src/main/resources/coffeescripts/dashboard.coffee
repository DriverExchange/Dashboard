
$ ->

	$("#topBar").replaceWith fwk.views.topBar({})

	$widgets = $("#widgets")
	nbCols = fwk.data.dashboardConf.widgets.length
	for cols, idx in fwk.data.dashboardConf.widgets
		$widgets.append("""<div class="col col#{idx} nbCols#{nbCols}"></div>""")
		$col = $widgets.find(".col.col#{idx}")
		for widgetName in cols
			$col.append(fwk.views.widget(widgetName: widgetName, title: fwk.data.widgetTitles[widgetName]))
			$col.find(".widget[data-name=#{widgetName}]").spinStart(largeSpinnerOptions)
			$.ajax
				url: "/widgets/#{widgetName}"
				success: (widget) ->
					$widget = $(".widget[data-name=#{widget.name}]")
					tables = ""
					for table in widget.configuration.tables
						tables += fwk.views.widgetTable(results: widget.data[table.queryName], table: table)
					$widget.replaceWith fwk.views.widget
						widgetName: widget.name
						widget: widget
						body: tables
				error: (xhr) ->
					if xhr.responseText[0] == "{" || xhr.responseText[0] == "["
						fwk.views.widget(errors: $.parseJSON(xhr.responseText))

