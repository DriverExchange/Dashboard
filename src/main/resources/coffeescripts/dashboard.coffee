
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
					body = ""
					if widget.css
						$("head").append """<style type="text/css">#{widget.css}</style>"""
					if widget.template
						fwk.views.addInline(widgetTemplate: widget.template)
						body = fwk.views.widgetTemplate(widget)
					else
						for table in widget.configuration.tables
							body += fwk.views.widgetTable(results: widget.data[table.queryName], table: table)
					$widget.replaceWith fwk.views.widget
						widgetName: widget.name
						widget: widget
						body: body
				error: (xhr) ->
					if xhr.responseText[0] == "{" || xhr.responseText[0] == "["
						fwk.views.widget(errors: $.parseJSON(xhr.responseText))

