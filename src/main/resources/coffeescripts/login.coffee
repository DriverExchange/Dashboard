
fwk.domEvents.add
	"#loginForm": submit: ->
		$("#loginForm button").spinStart(top: "13px", left: "-30px", length: 5,  radius: 4, lines: 7,  width: 4, color: "#cedeef")
		$.ajax
			type: "post"
			url: "/login"
			dataType: "json"
			data:
				email: $("input[name=email]").val()
				password: $("input[name=password]").val()
			success: ->
				window.location.href = "/"
			error: (xhr) ->
				$("#loginForm button").spinStop()
				displayedError = false
				if xhr.responseText && (xhr.responseText[0] == "{" || xhr.responseText[0] == "[")
					data = $.parseJSON(xhr.responseText)
					if data.error
						$("#loginForm .errorMessage").text(data.error)
						displayedError = true
				if !displayedError
					$("#loginForm .errorMessage").text("Unexpected error")

		false

$ ->
	$("input").first().focus()
