package com.smaservices.publication_api.repository;

import com.smaservices.publication_api.entity.ThirdPartyAccount;
import com.smaservices.publication_api.entity.enums.ThirdPartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ThirdPartyAccountRepository extends JpaRepository<ThirdPartyAccount, Long> {
    List<ThirdPartyAccount> findByUserId(Long userId);
    Optional<ThirdPartyAccount> findByUserIdAndType(Long userId, ThirdPartyType type);
}