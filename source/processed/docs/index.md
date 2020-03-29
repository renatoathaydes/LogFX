{{ define title "LogFX" }}
{{ define moduleName "Docs" }}
{{ define path baseURL + "/docs/index.html" }}
{{ include /processed/fragments/_header.html }}
{{ include /processed/fragments/_nav.html }}

<div class="title">LogFX Docs</div>


{{ for section /processed/docs }}
{{ if section.moduleName != moduleName }}
* [{{eval section.moduleName}}]({{ eval section.path }})
{{ end }}
{{ end }}

{{ include /processed/fragments/_footer.html }}
