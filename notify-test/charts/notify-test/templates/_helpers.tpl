{{/* common helpers */}}

{{/*
Concatenate to strings together for the purpose of naming a kubernetes resource. Resources must be <= 63 chars in length
so this helper will truncate the left hand string to ensure the suffix remains in-tact.
*/}}
{{- define "notifyTest.nameConcat" }}
{{- $maxK8sNameLenMinusSeperator := 62 }}
{{- $suffix := index . 1 | trimPrefix "-" }}
{{- $nameMaxLen := int (sub $maxK8sNameLenMinusSeperator (len $suffix)) }}
{{- $name := index . 0 | trimSuffix "-" | trunc $nameMaxLen }}
{{- printf "%s-%s" $name $suffix }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "notifyTest.name" -}}
{{- $maxK8sNameLen := 63 }}
{{- default .Chart.Name .Values.nameOverride | trunc $maxK8sNameLen | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "notifyTest.fullname" -}}
{{- $maxK8sNameLen := 63 }}
{{- $maxK8sNameLenMinusSeperator := 62 }}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc $maxK8sNameLen | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc $maxK8sNameLen | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc $maxK8sNameLenMinusSeperator | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "notifyTest.chart" }}
{{- $maxK8sNameLenMinusSeperator := 62 }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc $maxK8sNameLenMinusSeperator | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "notifyTest.labels" }}
helm.sh/chart: {{ include "notifyTest.chart" . }}
{{ include "notifyTest.selectorLabels" . }}
{{- if .Chart.Version }}
app.kubernetes.io/version: {{ .Chart.Version | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "notifyTest.selectorLabels" -}}
app.kubernetes.io/name: {{ include "notifyTest.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "notifyTest.serviceAccountName" }}
{{- if .Values.serviceAccount.create }}
{{- default (include "notifyTest.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
