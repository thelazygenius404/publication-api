package com.smaservices.publication_api.entity;

import com.smaservices.publication_api.entity.enums.AccountStatus;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "third_party_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "user_id",
                                "type"
                        }
                )
        }
)
public class ThirdPartyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThirdPartyType type;

    @Column(name = "site_url", length = 500)
    private String siteUrl;

    @Column(
            name = "access_token_enc",
            nullable = false,
            length = 2048
    )
    private String accessTokenEnc;

    @Column(
            name = "refresh_token_enc",
            length = 2048
    )
    private String refreshTokenEnc;
    @Column(
            name = "external_account_id",
            length = 255
    )
    private String externalAccountId;

    @Column(
            name = "access_token_expires_at"
    )
    private Instant accessTokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status =
            AccountStatus.CONNECTED;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ThirdPartyType getType() {
        return type;
    }

    public void setType(
            ThirdPartyType type) {
        this.type = type;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(
            String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public void setAccessTokenEnc(
            String accessTokenEnc) {
        this.accessTokenEnc =
                accessTokenEnc;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public void setRefreshTokenEnc(
            String refreshTokenEnc) {
        this.refreshTokenEnc =
                refreshTokenEnc;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(
            AccountStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(
            User user) {
        this.user = user;
    }
    public String getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(
            String externalAccountId) {

        this.externalAccountId =
                externalAccountId;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(
            Instant accessTokenExpiresAt) {

        this.accessTokenExpiresAt =
                accessTokenExpiresAt;
    }
}