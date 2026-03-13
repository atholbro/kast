{{/*
The name of the component.
*/}}
{{- define "notifyTest.receiver.name" -}}
{{- include "notifyTest.nameConcat" (list (include "notifyTest.name" .) "receiver") }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "notifyTest.receiver.fullname" }}
{{- include "notifyTest.nameConcat" (list (include "notifyTest.fullname" .) "receiver") }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "notifyTest.receiver.labels" }}
{{- include "notifyTest.labels" . }}
app.kubernetes.io/component: receiver
{{- end }}

{{/*
Selector labels
*/}}
{{- define "notifyTest.receiver.selectorLabels" -}}
{{- include "notifyTest.selectorLabels" . }}
app.kubernetes.io/component: receiver
{{- end -}}
