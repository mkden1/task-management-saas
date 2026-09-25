package org.example.dto;

public class AuthResponse {
    private String token;
    private String email;
    private Long userId;
    private Long organizationId;
    private String organizationName;
    private String role;

    public AuthResponse(String token, String email, Long userId, Long organizationId, String organizationName, String role) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
