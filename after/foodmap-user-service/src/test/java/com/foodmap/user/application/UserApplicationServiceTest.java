package com.foodmap.user.application;

import com.foodmap.common.security.CurrentUser;
import com.foodmap.user.domain.UserStatus;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.memory.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserApplicationServiceTest {

    @Test
    void returnsCurrentUserProfileFromPersistenceEntityWithoutExposingEntity() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        UserEntity entity = new UserEntity();
        entity.setUserId(1001L);
        entity.setAccountId(2001L);
        entity.setNickname("小张");
        entity.setAvatarMediaId(3001L);
        entity.setUserStatus(UserStatus.NORMAL.name());
        entity.setSearchable((short) 1);
        repository.save(entity);
        UserApplicationService service = new UserApplicationService(repository);

        CurrentUserResponse response = service.currentUser(new CurrentUser(1001L, 2001L, "foodie_01"));

        assertThat(response).isNotInstanceOf(UserEntity.class);
        assertThat(response.userId()).isEqualTo(1001L);
        assertThat(response.nickname()).isEqualTo("小张");
        assertThat(response.userStatus()).isEqualTo(UserStatus.NORMAL.name());
    }
}
