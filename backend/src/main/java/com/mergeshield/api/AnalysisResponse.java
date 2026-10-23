package com.mergeshield.api;
import java.util.List;
public record AnalysisResponse(String status, int breakingCount, int warningCount, List<Finding> findings) {}
