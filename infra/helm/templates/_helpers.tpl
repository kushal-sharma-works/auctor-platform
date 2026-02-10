{{- define "auctor.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "auctor.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "auctor.serviceName" -}}
{{- printf "%s-%s" (include "auctor.fullname" .context) .name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "auctor.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
{{- end -}}

{{- define "auctor.labels" -}}
{{- include "auctor.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .context.Release.Service }}
app.kubernetes.io/part-of: {{ include "auctor.name" .context }}
{{- end -}}
