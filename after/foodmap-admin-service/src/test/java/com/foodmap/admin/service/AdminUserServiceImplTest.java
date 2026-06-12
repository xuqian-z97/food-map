package com.foodmap.admin.service;

import com.foodmap.admin.domain.AdminStatus;
import com.foodmap.admin.dto.AdminUserResponse;
import com.foodmap.admin.dto.CreateAdminUserRequest;
import com.foodmap.admin.infrastructure.persistence.entity.AdminUserEntity;
import com.foodmap.admin.infrastructure.persistence.memory.InMemoryAdminUserRepository;
import com.foodmap.admin.service.impl.AdminUserServiceImpl;
import com.foodmap.common.exception.FoodMapException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminUserServiceImplTest {

    @Test
    void createsActiveAdminUserWithHashedPasswordAndDtoResponse() {
        InMemoryAdminUserRepository repository = new InMemoryAdminUserRepository();
        AdminUserService service = new AdminUserServiceImpl(
                new TestAdminBusinessIdGenerator(),
                repository,
                new Pbkdf2AdminPasswordHashService("test-pepper")
        );

        AdminUserResponse response = service.createAdminUser(new CreateAdminUserRequest(
                "ops_admin",
                "secret123",
                "运营管理员",
                "13800138000",
                "ops@example.com"
        ));

        AdminUserEntity stored = repository.findByUsername("ops_admin").orElseThrow();
        assertThat(response).isNotInstanceOf(AdminUserEntity.class);
        assertThat(response.adminUserId()).isEqualTo(stored.getAdminUserId());
        assertThat(response.username()).isEqualTo("ops_admin");
        assertThat(response.adminStatus()).isEqualTo(AdminStatus.ACTIVE.name());
        assertThat(response.permissionVersion()).isEqualTo(1L);
        assertThat(response.mobileMasked()).isEqualTo("138****8000");
        assertThat(response.emailMasked()).isEqualTo("o***@example.com");
        assertThat(stored.getPasswordHash()).isNotBlank();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(stored.getPasswordHash()).startsWith("PBKDF2WithHmacSHA256$");
    }

    @Test
    void rejectsDuplicateActiveUsername() {
        InMemoryAdminUserRepository repository = new InMemoryAdminUserRepository();
        AdminUserService service = new AdminUserServiceImpl(
                new TestAdminBusinessIdGenerator(),
                repository,
                new Pbkdf2AdminPasswordHashService("test-pepper")
        );
        CreateAdminUserRequest request = new CreateAdminUserRequest(
                "ops_admin",
                "secret123",
                "运营管理员",
                null,
                null
        );

        service.createAdminUser(request);

        assertThatThrownBy(() -> service.createAdminUser(request))
                .isInstanceOf(FoodMapException.class);
    }
}
