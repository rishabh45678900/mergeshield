package com.mergeshield.api;
public record Finding(String ruleId, String severity, String location, String summary, String oldValue, String newValue, String evidence) {}
