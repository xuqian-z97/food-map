package com.foodmap.user.service;

import com.foodmap.common.security.CurrentUser;
import com.foodmap.common.exception.FoodMapException;
import com.foodmap.user.application.port.UserBusinessIdGenerator;
import com.foodmap.user.domain.UserStatus;
import com.foodmap.user.dto.CurrentUserResponse;
import com.foodmap.user.dto.ProvisionUserRequest;
import com.foodmap.user.infrastructure.persistence.entity.UserEntity;
import com.foodmap.user.infrastructure.persistence.memory.InMemoryUserRepository;
import com.foodmap.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceImplTest {

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
        UserService service = new UserServiceImpl(repository, new TestUserBusinessIdGenerator());

        CurrentUserResponse response = service.currentUser(new CurrentUser(1001L, 2001L, "foodie_01"));

        assertThat(response).isNotInstanceOf(UserEntity.class);
        assertThat(response.userId()).isEqualTo(1001L);
        assertThat(response.nickname()).isEqualTo("小张");
        assertThat(response.userStatus()).isEqualTo(UserStatus.NORMAL.name());
    }

    @Test
    void provisionsUserProfileAndDefaultSettingsForRegisteredAccount() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        UserService service = new UserServiceImpl(repository, new TestUserBusinessIdGenerator());

        CurrentUserResponse provisioned = service.provisionUser(new ProvisionUserRequest(2001L, 1001L, "小张"));
        CurrentUserResponse current = service.currentUser(new CurrentUser(1001L, 2001L, "foodie_01"));

        assertThat(provisioned.userId()).isEqualTo(1001L);
        assertThat(provisioned.accountId()).isEqualTo(2001L);
        assertThat(provisioned.nickname()).isEqualTo("小张");
        assertThat(current.nickname()).isEqualTo("小张");
        assertThat(repository.findProfileByUserId(1001L)).isPresent();
        assertThat(repository.findSettingByUserId(1001L)).isPresent();
        assertThat(repository.findSettingByUserId(1001L).orElseThrow().getDefaultVisibilityType()).isEqualTo("PRIVATE");
    }

    @Test
    void rejectsCurrentUserWhenAccountIdDoesNotMatchUserProfile() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        UserEntity entity = new UserEntity();
        entity.setUserId(1001L);
        entity.setAccountId(2001L);
        entity.setNickname("小张");
        entity.setUserStatus(UserStatus.NORMAL.name());
        repository.save(entity);
        UserService service = new UserServiceImpl(repository, new TestUserBusinessIdGenerator());

        assertThatThrownBy(() -> service.currentUser(new CurrentUser(1001L, 2999L, "foodie_01")))
                .isInstanceOf(FoodMapException.class)
                .hasMessageContaining("当前账号与用户资料不匹配");
    }

    private static class TestUserBusinessIdGenerator implements UserBusinessIdGenerator {
        @Override
        public Long nextProfileId() {
            return 3001L;
        }

        @Override
        public Long nextSettingId() {
            return 4001L;
        }
    }
}
