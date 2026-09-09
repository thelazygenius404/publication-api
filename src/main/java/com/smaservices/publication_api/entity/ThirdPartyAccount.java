package com.smaservices.publication_api.entity;

import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import jakarta.persistence.*;

@Entity
@Table(name = "third_party_accounts")
public class ThirdPartyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThirdPartyType type;

    // Stockage de la version chiffrée (enc) du token
    @Column(nullable = false, length = 1024)
    private String accessTokenEnc;

    @Column(length = 1024)
    private String refreshTokenEnc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.CONNECTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ajoutez les Getters et Setters standard
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ThirdPartyType getType() { return type; }
    public void setType(ThirdPartyType type) { this.type = type; }
    public String getAccessTokenEnc() { return accessTokenEnc; }
    public void setAccessTokenEnc(String accessTokenEnc) { this.accessTokenEnc = accessTokenEnc; }
    public String getRefreshTokenEnc() { return refreshTokenEnc; }
    public void setRefreshTokenEnc(String refreshTokenEnc) { this.refreshTokenEnc = refreshTokenEnc; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}