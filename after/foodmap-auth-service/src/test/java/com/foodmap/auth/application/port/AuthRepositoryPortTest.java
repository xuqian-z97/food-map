package com.foodmap.auth.application.port;

import com.foodmap.auth.infrastructure.persistence.entity.AuthAccountEntity;
import com.foodmap.auth.infrastructure.persistence.memory.InMemoryAuthAccountRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRepositoryPortTest {

    @Test
    void memoryRepositoryImplementsAuthAccountPortForApplicationTests() {
        AuthAccountRepository repository = new InMemoryAuthAccountRepository();
        AuthAccountEntity entity = new AuthAccountEntity();
        entity.setAccountId(1001L);
        entity.setUserId(2001L);
        entity.setAccountName("foodie_01");
        entity.setAccountStatus("NORMAL");
        entity.setIsDelete((short) 0);

        repository.save(entity);

        assertThat(repository.findByLoginIdentifier("foodie_01")).contains(entity);
        assertThat(repository.findByAccountId(1001L)).contains(entity);
    }
}
