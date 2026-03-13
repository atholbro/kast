{{/*
The name of the component.
*/}}
{{- define "notifyTest.sender.name" -}}
{{- include "notifyTest.nameConcat" (list (include "notifyTest.name" .) "sender") }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "notifyTest.sender.fullname" }}
{{- include "notifyTest.nameConcat" (list (include "notifyTest.fullname" .) "sender") }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "notifyTest.sender.labels" }}
{{- include "notifyTest.labels" . }}
app.kubernetes.io/component: sender
{{- end }}

{{/*
Selector labels
*/}}
{{- define "notifyTest.sender.selectorLabels" -}}
{{- include "notifyTest.selectorLabels" . }}
app.kubernetes.io/component: sender
{{- end -}}
