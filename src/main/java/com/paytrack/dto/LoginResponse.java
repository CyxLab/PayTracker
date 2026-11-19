package com.paytrack.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInMs) {}