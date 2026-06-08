package com.foodmap.user.application.port;

import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.memory.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryPortTest {

    @Test
    void memoryRepositoryImplementsUserPortForApplicationTests() {
        UserRepository repository = new InMemoryUserRepository();
        UserEntity entity = new UserEntity();
        entity.setUserId(2001L);
        entity.setAccountId(1001L);
        entity.setNickname("小张");
        entity.setUserStatus("NORMAL");
        entity.setSearchable((short) 1);

        repository.save(entity);

        assertThat(repository.findByUserId(2001L)).contains(entity);
    }
}
